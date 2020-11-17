var app = angular.module("app", ["ui.router"]);
//加载所有component
let dependencies = [
    "entityTable","buttonList","query","bulmaTable","menu","pagination","paginationTable","formModal"
];
for(let i=0;i<dependencies.length;i++){
    dependencies[i] = "./component/"+dependencies[i]+"/"+dependencies[i]+".js";
}
$script(dependencies);

app.config(function ($controllerProvider, $compileProvider, $filterProvider, $provide, $stateProvider, $urlRouterProvider) {
    app.register = {
        controller: $controllerProvider.register,
        component: $compileProvider.component,
        directive: $compileProvider.directive,
        filter: $filterProvider.register,
        factory: $provide.factory,
        service: $provide.service
    };

    $urlRouterProvider.otherwise("/history");
    $stateProvider.state("menu", {
        url: "/{name}",
        templateUrl: function ($stateParams) {
            return "./app/" + $stateParams.name + "/" + $stateParams.name + ".html";
        },
        params: {
            "name": null,
            "api": null,
            "entity": null,
        },
        resolve: {
            load: function ($stateParams, $rootScope, $q) {
                var jsUrl = "./app/" + $stateParams.name + "/" + $stateParams.name + ".js";
                var deferred = $q.defer();
                var dependencies = angular.copy(jsUrl);
                $script(dependencies, function () {
                    $rootScope.$apply(function () {
                        deferred.resolve();
                    });
                });
                switch($stateParams.name){
                    case "history":{
                        $rootScope.tabMap[$stateParams.name] = angular.copy($stateParams);
                        $rootScope.tabMap[$stateParams.name]["tabName"]="文档历史";
                    }break;
                    case "setting":{
                        $rootScope.tabMap[$stateParams.name] = angular.copy($stateParams);
                        $rootScope.tabMap[$stateParams.name]["tabName"]="文档设置";
                    }break;
                    case "collect":{
                        $rootScope.tabMap[$stateParams.name] = angular.copy($stateParams);
                        $rootScope.tabMap[$stateParams.name]["tabName"]="收藏管理";
                    }break;
                    case "entity":{
                        $rootScope.tabMap[$stateParams.name+"_"+$stateParams.entity.className] = angular.copy($stateParams);
                        $rootScope.tabMap[$stateParams.name+"_"+$stateParams.entity.className]["tabName"]=$stateParams.entity.simpleName+($stateParams.entity.description?"("+$stateParams.entity.description+")":'');
                    }break;
                    case "api":{
                        $rootScope.tabMap[$stateParams.name+"_"+$stateParams.api.name] = angular.copy($stateParams);
                        $rootScope.tabMap[$stateParams.name+"_"+$stateParams.api.name]["tabName"]=$stateParams.api.name;
                    }break;
                    default:{}break;
                }
                $rootScope.currentViewName = $stateParams.name;
                $rootScope.currentAPI = $stateParams.api;
                $rootScope.currentEntity = $stateParams.entity;
                return deferred.promise;
            }
        }
    });
});

app.factory('formInterceptor', function ($q, $rootScope) {
    return {
        request: function (config) {
            config.target = $rootScope.target;
            angular.element(config.target).addClass("is-loading");
            return config;
        },
        requestError: function (err) {
            console.log(err);
            angular.element(err.config.target).removeClass("is-loading");
        },
        response: function (res) {
            angular.element(res.config.target).removeClass("is-loading");
            return res;
        },
        responseError: function (err) {
            angular.element(err.config.target).removeClass("is-loading");
            if(err.data&&err.data.message){
                $rootScope.showErrorNotification(err.data.message);
            }
            return $q.reject(err);
        }
    }
});

app.config(function ($httpProvider) {
    $httpProvider.interceptors.push('formInterceptor');
});

app.filter('trustAsHtml', function ($sce) {
    return function (value) {
        return $sce.trustAsHtml(value);
    };
});

app.filter('since', function () {
    return function (lastUpdateTime) {
        let duration = Math.floor((new Date().getTime() - lastUpdateTime) / 1000);
        if (duration < 60) {
            return duration + "秒前";
        } else if (duration < 3600) {
            return Math.floor(duration / 60) + "分钟前";
        } else if (duration < 24 * 3600) {
            return Math.floor(duration / 60 / 60) + "小时前";
        } else if(duration < 24 * 3600 * 365){
            return Math.floor(duration / 60 / 60 / 24) + "天前";
        } else {
            return Math.floor(duration / 60 / 60 / 24 / 365) + "年前";
        }
    };
});

app.factory("$storageService",function(){
    let storageService = {};
    var storageServiceProxy = new Proxy(storageService,{
        get : function(target,key,receive){
            let storageKey = location.host+location.pathname;
            let storageObject = JSON.parse(localStorage.hasOwnProperty(storageKey)?localStorage.getItem(storageKey):"{}");
            return storageObject[key];
        },
        set : function(target,key,value,receive){
            let storageKey = location.host+location.pathname;
            let storageObject = JSON.parse(localStorage.hasOwnProperty(storageKey)?localStorage.getItem(storageKey):"{}");
            storageObject[key] = value;
            localStorage.setItem(storageKey,JSON.stringify(storageObject));
        },
        has : function(target, key){
            let storageKey = location.host+location.pathname;
            let storageObject = JSON.parse(localStorage.hasOwnProperty(storageKey)?localStorage.getItem(storageKey):"{}");
            return storageObject.hasOwnProperty(key);
        },
        ownKeys : function(target){
            let storageKey = location.host+location.pathname;
            let storageObject = JSON.parse(localStorage.hasOwnProperty(storageKey)?localStorage.getItem(storageKey):"{}");
            return Object.keys(storageObject);
        }
    });
    return storageServiceProxy;
});

app.run(function ($rootScope,$state,$storageService,$http) {
    $rootScope.target = null;
    document.addEventListener("click",function(event){
        $rootScope.target = event.target;
    },true);

    $rootScope.apiDocument = apiDocument;
    $rootScope.getAPI = function(name){
        for(let i=0;i<$rootScope.apiDocument.apiControllerList.length;i++){
            let apiList = $rootScope.apiDocument.apiControllerList[i].apiList;
            for(let j=0;j<apiList.length;j++){
                let historyName = apiList[j].methods[0]+"_"+apiList[j].url+"_"+apiList[j].description;
                if(name===historyName){
                    return apiList[j];
                }
            }
        }
        return null;
    };

    //处理历史变更记录
    for(let i=0;i<$rootScope.apiDocument.apiHistoryList.length;i++){
        let addList = $rootScope.apiDocument.apiHistoryList[i].addList;
        let modifyList = $rootScope.apiDocument.apiHistoryList[i].modifyList;
        for(let l=0;l<addList.length;l++){
            addList[l] = $rootScope.getAPI(addList[l]);
        }
        for(let l=0;l<modifyList.length;l++){
            modifyList[l] = $rootScope.getAPI(modifyList[l]);
        }

        //剔除已经不存在的接口
        for(let l=0;l<addList.length;l++){
            if(typeof(addList[l])=="string"){
                addList = addList.splice(l,1);
            }
        }
        for(let l=0;l<modifyList.length;l++){
            if(typeof(modifyList[l])=="string"){
                modifyList = modifyList.splice(l,1);
            }
        }
    }

    //tab标签页
    $rootScope.tabMap = {};
    $rootScope.changeToTab = function(key){
        let val = $rootScope.tabMap[key];
        $state.go("menu",$rootScope.tabMap[key]);
    };
    $rootScope.closeTab = function(key){
        delete $rootScope.tabMap[key];
    };

    $rootScope.copyToClipBoard = function(value){
        let e = document.getElementById("textarea");
        e.value = value;
        e.select();
        document.execCommand("copy");
    };

    /**
     * 刷新时执行请求
     * @param api 请求api信息
     * @param request 请求参数信息
     * */
    $rootScope.executeOnRefresh = function(api,request,callbackFunction){
        let operation = {
            url:api.url
        };
        //处理PathVariable参数
        for(let i=0;i<api.apiParameters.length;i++){
            let apiParameter = api.apiParameters[i];
            if(apiParameter.position==="path"){
                operation.url = operation.url.replace("{"+apiParameter.name+"}",request[apiParameter.name]);
                delete request[apiParameter.name];
            }
        }
        //处理请求方法
        let method = api.methods[0];
        if(method==="all"){
            method = "POST";
        }
        operation.method = method;
        //处理Content-Type
        if(method==="POST"||method==="PUT"||method==="PATCH"){
            if(api.contentType.indexOf("multipart/form-data")>=0){
                let fd = new FormData();
                for(let prop in request){
                    fd.append(prop,request[prop]);
                }
                operation.data = fd;
            }else if(api.contentType.indexOf("application/json")>=0){
                for(let prop in request){
                    operation.data = request[prop];
                }
            }else{
                operation.data += $httpParamSerializer(request);
            }
        }else{
            operation.params = request;
        }
        operation.headers = {"Content-Type":api.contentType};
        if(api.contentType.indexOf("multipart/form-data")>=0){
            operation.headers = {"Content-Type":undefined};
        }
        //添加全局头部
        for(let i=0;i<$storageService.settings.globalHeaders.length;i++){
            let globalHeader = $storageService.settings.globalHeaders[i];
            operation.headers[globalHeader.key] = globalHeader.value;
        }
        $http(operation).then(function(response){
            console.log(response);
            //执行回调函数
            let fn = new Function("data","globalHeaders",callbackFunction.substring(callbackFunction.indexOf("{")+1,callbackFunction.lastIndexOf("}")));
            let settings = $storageService.settings;
            fn(response.data,settings.globalHeaders);
            $storageService.settings = settings;
        },function(error){
            console.log(error);
        }).finally(function(){
        });
    };

    //刷新时执行
    let keys = Object.getOwnPropertyNames($storageService).filter(x => x.indexOf("API_")==0);
    for(let i=0;i<keys.length;i++){
        let apiStorage = $storageService[keys[i]];
        let saveParameterList = apiStorage.saveParameterList;
        if(saveParameterList&&saveParameterList.length>0){
            for(let j=0;j<saveParameterList.length;j++){
                if(saveParameterList[j].executeOnRefresh){
                    console.log("[刷新时执行]"+apiStorage.api.url+",name:"+saveParameterList[j].name);
                    $rootScope.executeOnRefresh(apiStorage.api,saveParameterList[j].request,apiStorage.callbackFunction);
                }
            }
        }
    }

    if(null==$storageService.collectionList){
        $storageService.collectionList = [];
    }
    $rootScope.collectionList = $storageService.collectionList;
    if(null==$storageService.recentUsedList){
        $storageService.recentUsedList = [];
    }
    $rootScope.recentUsedList = $storageService.recentUsedList;
    let settings = $storageService.settings;
    if(null==settings){
        $storageService.settings = {
            //显示接口测试界面
            "showTestView":false,
            //显示实体类信息
            "showEntity":false,
            //显示接口实体类信息
            "showAPIEntity":false,
            //显示最近使用历史记录
            "showRecentUsed":false,
            //全局头部
            "globalHeaders":[],
            //环境列表
            "environments":[],
            //当前环境
            "currentEnvironment":null
        };
    }
});

app.controller("menuController",function($scope,$rootScope,$state,$storageService){
    //API搜索功能
    $scope.searchText = "";
    $scope.showEntity = function(className){
        if($scope.searchText===""){
            return true;
        }
        return className.indexOf($scope.searchText)>=0;
    };
    //过滤APIController
    $scope.showApiController = function(apiController){
        if($scope.searchText===""){
            return true;
        }
        if(apiController.name.indexOf($scope.searchText)>=0){
            return true;
        }
        let apiList = apiController.apiList;
        for(let i=0;i<apiList.length;i++){
            if($scope.showApi(apiController,apiList[i])){
                return true;
            }
        }
        return false;
    };
    //过滤API
    $scope.showApi = function(apiController,api){
        if($scope.searchText===""){
            return true;
        }
        if(apiController.name.indexOf($scope.searchText)>=0){
            return true;
        }
        if(api.name.indexOf($scope.searchText)>=0||api.url.indexOf($scope.searchText)>=0){
            return true;
        }
        return false;
    };

    $scope.storageService = $storageService;
});