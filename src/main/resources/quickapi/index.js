let app = angular.module("app",[]);
app.factory('formInterceptor', function ($q, $rootScope) {
    return {
        request: function (config) {
            $rootScope.isLoading = true;
            return config;
        },
        requestError: function (err) {
            console.log(err);
            $rootScope.isLoading = false;
        },
        response: function (res) {
            $rootScope.isLoading = false;
            return res;
        },
        responseError: function (err) {
            $rootScope.isLoading = false;
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
app.directive('entityTable',function(){
    return {
        restrict: 'AE',
        replace: true,
        templateUrl:"directive/entityTable.html",
        scope:{
            "entity":"=entity"
        },
        link:function($scope,$elem,$attr){
        }
    }
});
app.controller("indexController",function($scope,$rootScope,$http,$httpParamSerializer,$location,$anchorScroll){
    $scope.apiDocument = null;
    $http.get("api.json").then(function(response){
        $scope.apiDocument = response.data;
        //处理历史变更记录
        for(let i=0;i<$scope.apiDocument.apiHistoryList.length;i++){
            let addList = $scope.apiDocument.apiHistoryList[i].addList;
            let modifyList = $scope.apiDocument.apiHistoryList[i].modifyList;
            for(let i=0;i<$scope.apiDocument.apiControllerList.length;i++){
                let apiList = $scope.apiDocument.apiControllerList[i].apiList;
                for(let j=0;j<apiList.length;j++){
                    let historyName =  $scope.apiDocument.apiControllerList[i].className+"#"+apiList[j].methods[0]+"_"+apiList[j].url;
                    for(let l=0;l<addList.length;l++){
                        if(addList[l]===historyName){
                            addList[l] = apiList[j];
                        }
                    }
                    for(let l=0;l<modifyList.length;l++){
                        if(modifyList[l]===historyName){
                            modifyList[l] = apiList[j];
                        }
                    }
                }
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
    });

    /**
     * 从本地存储中获取
     * */
    $scope.getFromLocalStorage = function(key,defaultValue){
        if(null!=localStorage.getItem(key)){
            return JSON.parse(localStorage.getItem(key));
        }
        return defaultValue;
    };
    /**
     * 保存到本地存储
     * */
    $scope.saveToLocalStorage = function(key,value){
        if(typeof(value)!="undefined"&&null!=value){
            localStorage.setItem(key,JSON.stringify(value));
        }
    };

    //tab页设置
    $scope.activeTabName = "";
    $scope.tabMap = {
        "/quickapi/history":{
            "name": "文档历史",
            "view": "history"
        }
    };
    $scope.changeToTab = function(key){
        let val = $scope.tabMap[key];
        $scope.view = val.view;
        $scope.activeTabName = key;
        switch($scope.view){
            case "entity":{
                $scope.currentEntity = val.entity;
            };break;
            case "api":{
                $scope.currentAPI = val.api;
            };break;
        }
    };
    $scope.closeTab = function(key){
        delete $scope.tabMap[key];
    };

    //文档历史
    $scope.view = "history";
    $scope.showHistory = function(){
        $scope.view = "history";
        $scope.tabMap["/quickapi/history"] = {
            "name": "文档历史",
            "view": "history"
        };
        $scope.activeTabName = "/quickapi/history";
    };

    //全局头部
    $scope.showGlobalHeaders = function(){
        $scope.view = "globalHeader";
        $scope.tabMap["/quickapi/globalHeader"] = {
            "name": "全局头部",
            "view": "globalHeader"
        };
        $scope.activeTabName = "/quickapi/globalHeader";
    };
    $scope.headers = $scope.getFromLocalStorage("headers",{});
    $scope.addHeader = function(){
        if(typeof($scope.newHeaderKey)!="undefined"&&""!=$scope.newHeaderKey){
            $scope.headers[$scope.newHeaderKey] = $scope.newHeaderValue;
            $scope.newHeaderKey = "";
            $scope.newHeaderValue = "";
            $scope.saveToLocalStorage("headers",$scope.headers);
        }
    };
    $scope.removeHeader = function(key){
        delete $scope.headers[key];
        $scope.saveToLocalStorage("headers",$scope.headers);
    };

    //环境设置
    $scope.showEnvironment = function(){
        $scope.view = "environment";
        $scope.tabMap["/quickapi/environment"] = {
            "name": "环境设置",
            "view": "environment"
        };
        $scope.activeTabName = "/quickapi/environment";
    };

    $scope.environmentList = $scope.getFromLocalStorage("environmentList",[]);
    $scope.toggleEnvironment = function($event,environment){
        if($event.target.checked){
            environment.enable = true;
        }else{
            environment.enable = true;
        }
        $scope.saveToLocalStorage("environmentList",$scope.environmentList);
    };

    $scope.newEnvironment = {
        "host":"",
        "mode":"0",
        "enable":false
    };
    $scope.addEnvironment = function(){
        if(null==$scope.newEnvironment.host||$scope.newEnvironment.host==""){
            alert("远程地址不能为空!");
            return;
        }
        if($scope.newEnvironment.host.indexOf("http")!=0){
            alert("远程地址需以http开头");
            return;
        }

        $scope.environmentList.push($scope.newEnvironment);
        $scope.newEnvironment = {
            "host":"",
            "mode":"0",
            "enable":false
        };
        $scope.saveToLocalStorage("environmentList",$scope.environmentList);
    };
    $scope.removeEnvironment = function(index){
        if(confirm("确定删除吗?")){
            $scope.environmentList.splice(index,1);
            $scope.saveToLocalStorage("environmentList",$scope.environmentList);
        }
    };

    //实体类显示
    $scope.currentEntity = null;
    $scope.setCurrentEntity = function(entity){
        $scope.view = "entity";
        $scope.tabMap["/quickapi/entity/"+entity.className] = {
            "name": entity.simpleName,
            "view": "entity",
            "entity":entity
        };
        $scope.activeTabName = "/quickapi/entity/"+entity.className;

        $scope.currentEntity = entity;
        $location.hash("top");
        $anchorScroll();
    };

    //API搜索
    $scope.searchText = "";
    //过滤APIController
    $scope.showApiController = function(apiController){
        if($scope.searchText===""){
            return true;
        }
        let apiList = apiController.apiList;
        for(let i=0;i<apiList.length;i++){
            if($scope.showApi(apiList[i])){
                return true;
            }
        }
        return false;
    };
    //过滤API
    $scope.showApi = function(api){
        if($scope.searchText===""){
            return true;
        }
        if(api.name.indexOf($scope.searchText)>=0||api.url.indexOf($scope.searchText)>=0){
            return true;
        }
        return false;
    };

    //显示API详情
    $scope.currentAPI = null;
    $scope.setCurrentAPI = function(api){
        $scope.view = "api";
        $scope.tabMap[api.url+api.methods[0]] = {
            "name": api.name,
            "view": "api",
            "api": api
        };
        $scope.activeTabName = api.url+api.methods[0];

        $scope.currentAPI = api;
        $scope.response = null;

        $scope.request = {};
        let apiParameters = $scope.currentAPI.apiParameters;
        for(let i=0;i<apiParameters.length;i++){
            $scope.request[apiParameters[i].name] = apiParameters[i].defaultValue;
            if("textarea"==apiParameters[i].requestType&&$scope.apiDocument.apiEntityMap.hasOwnProperty(apiParameters[i].type)){
                let data = JSON.parse($scope.apiDocument.apiEntityMap[apiParameters[i].type].instance);
                $scope.request[apiParameters[i].name] = JSON.stringify(data,null,4);
            }
        }
        let requestValue = localStorage.getItem($scope.currentAPI.url);
        if(null!=requestValue&&""!=requestValue){
            $scope.request = JSON.parse(requestValue);
        }

        $location.hash("top");
        $anchorScroll();
    };

    $scope.request = {};
    $scope.response = null;

    //最近使用
    $scope.lastUsed  = $scope.getFromLocalStorage("lastUsed",[]);
    $scope.cleanHistory = function(){
        if(confirm("确认清空历史记录吗?")){
            $scope.lastUsed  = [];
            $scope.saveToLocalStorage("lastUsed",$scope.lastUsed);
        }
    };
    //执行请求
    $scope.execute = function(){
        //检查必填项
        let apiParameters = $scope.currentAPI.apiParameters;
        for(let i=0;i<apiParameters.length;i++){
            let apiParameter = apiParameters[i];
            if(apiParameter.requestType==="file"){
                if(apiParameter.required&&document.getElementById(apiParameter.name).files.length===0){
                    alert("请填写必填项:"+apiParameter.name);
                    return;
                }
            }else{
                let value = $scope.request[apiParameter.name];
                if(apiParameter.required&&(typeof(value)=="undefined"||value==="")){
                    alert("请填写必填项:"+apiParameter.name);
                    return;
                }
            }
        }

        //设置访问url
        let url = $scope.currentAPI.url;
        for(let i=0;i<$scope.environmentList.length;i++){
            if($scope.environmentList[i].enable){
                switch($scope.environmentList[i].mode){
                    case "0":{
                        //直连
                        url = $rootScope.choosedEnvironment.host+url;
                    }break;
                    // case "1":{
                    //     //中转
                    //     url = "/api/proxy/forwardHttpRequest?proxyUrl="+$scope.environmentList[i].host+url;
                    // }break;
                    default:{
                        alert("不支持的模式!mode:"+$scope.environmentList[i].mode);
                    }break;
                }
                break;
            }
        }

        let operation = {
            url:url
        };
        //处理路径
        for(let i=0;i<apiParameters.length;i++){
            let apiParameter = apiParameters[i];
            if(apiParameter.position==="path"){
                operation.url = operation.url.replace("{"+apiParameter.name+"}",$scope.request[apiParameter.name]);
                delete $scope.request[apiParameter.name];
            }
        }
        operation.url = $scope.apiDocument.prefix+operation.url;
        let method = $scope.currentAPI.methods[0];
        if(method==="all"){
            method = "POST";
        }
        operation.method = method;
        if(method==="POST"||method==="PUT"||method==="PATCH"){
            if($scope.currentAPI.contentType.indexOf("multipart/form-data")>=0){
                let fd = new FormData();
                for(let prop in $scope.request){
                    if(null!=document.getElementById(prop)){
                        fd.append(prop,document.getElementById(prop).files[0]);
                    }else{
                        fd.append(prop,$scope.request[prop]);
                    }
                }
                operation.data = fd;
            }else if($scope.currentAPI.contentType.indexOf("application/json")>=0){
                operation.data = $scope.request[$scope.currentAPI.apiParameters[0].name];
            }else{
                operation.data = $httpParamSerializer($scope.request);
            }
        }else{
            operation.params = $scope.request;
        }
        operation.headers = {"Content-Type":$scope.currentAPI.contentType};
        if($scope.currentAPI.contentType.indexOf("multipart/form-data")>=0){
            operation.headers = {"Content-Type":undefined};
        }
        for(let prop in $rootScope.headers){
            operation.headers[prop] = $rootScope.headers[prop];
        }
        $scope.loading = true;
        $http(operation).then(function(response){
            $scope.response = response;
            $scope.responseJSON = JSON.stringify(response.data,null,4);
        },function(error){
            $scope.response = error;
            $scope.responseJSON = JSON.stringify(error.data,null,4);
        }).finally(function(){
            $scope.loading = false;
            localStorage.setItem($scope.currentAPI.url,JSON.stringify($scope.request));
            if($scope.lastUsed.length>5){
                $scope.lastUsed.shift();
            }
            //判断是否有重复
            let exist = false;
            for(let i=0;i<$scope.lastUsed.length;i++){
                if($scope.lastUsed[i].url==$scope.currentAPI.url){
                    exist = true;
                    break;
                }
            }
            if(!exist){
                $scope.lastUsed.unshift($scope.currentAPI);
                $scope.saveToLocalStorage("lastUsed",$scope.lastUsed);
            }
        });
    };
});
