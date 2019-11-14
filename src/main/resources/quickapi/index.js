let app = angular.module("app",["ngSanitize"]);
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
app.run(function($rootScope){
    $rootScope.headers = {};
    $rootScope.showHeaderModal = function(){
        $rootScope.showHeader = true;
    };
    $rootScope.hideHeaderModal = function(){
        $rootScope.showHeader = false;
    };
    $rootScope.addHeader = function(){
        if(typeof($rootScope.addKey)!="undefined"&&""!=$rootScope.addKey){
            $rootScope.headers[$rootScope.addKey]=$rootScope.addValue;
            $rootScope.addKey = "";
            $rootScope.addValue = "";
        }
    };
    $rootScope.removeHeader = function(key){
        delete $rootScope.headers[key];
    };
});
app.controller("indexController",function($scope,$rootScope,$http,$httpParamSerializer,$location,$anchorScroll){
    $scope.apiDocument = {};
    $scope.apiControllerList = [];
    $http.get(location.pathname.substring(0,location.pathname.lastIndexOf("/"))+"/api.json?v="+new Date().getTime()).then(function(response){
        $scope.apiDocument = response.data;
        $scope.apiControllerList = $scope.apiDocument.apiControllerList;
        for(let i=0;i<$scope.apiControllerList.length;i++){
            let apiList = $scope.apiControllerList[i].apiList;
            for(let j=0;j<apiList.length;j++){
                let value = localStorage.getItem(apiList[j].url+"_state");
                if(value&&value=="true"){
                    apiList[j].ok = true;
                }else{
                    apiList[j].ok = false;
                }
            }
        }
        $scope.refreshAccessState();
    });

    $scope.showHistory = function(){
        $scope.currentEntity=null;
        $scope.currentAPI=null;
    };

    //服务状态
    $scope.isLoading = false;
    $scope.canAccess = true;
    $scope.refreshAccessState = function(){
        $scope.isLoading = true;
        $http.get($scope.apiControllerList[0].apiList[0].url).then(function(response){
            console.log(response);
            $scope.canAccess = true;
        },function(response){
            console.log(response);
            if(response.status===502){
                $scope.canAccess = false;
            }else{
                $scope.canAccess = true;
            }
        }).finally(function(){
            $scope.isLoading = false;
        });
    };

    //当前API信息
    $scope.currentEntity = null;
    $scope.setCurrentEntity = function(entity){
        $scope.currentAPI = null;
        $scope.currentEntity = entity;

        $location.hash("top");
        $anchorScroll();
    };

    $scope.currentAPI = null;
    $scope.setCurrentAPI = function(api){
        $scope.currentEntity = null;
        $scope.currentAPI = api;
        $scope.request = {};
        let apiParameters = $scope.currentAPI.apiParameters;
        for(let i=0;i<apiParameters.length;i++){
            $scope.request[apiParameters[i].name] = apiParameters[i].defaultValue;
            if(null!=apiParameters[i].exampleEntity){
                $scope.request[apiParameters[i].name] = $scope.apiDocument.apiEntityMap[apiParameters[i].exampleEntity].instance;
            }
        }
        $scope.response = null;
        $scope.result = null;

        let requestValue = localStorage.getItem($scope.currentAPI.url);
        if(null!=requestValue&&""!=requestValue){
            $scope.request = JSON.parse(requestValue);
        }

        $location.hash("top");
        $anchorScroll();
    };

    $scope.toggleAPIState = function(api){
        api.ok = !api.ok;
        localStorage.setItem(api.url+"_state",api.ok+"");
    };
    $scope.request = null;
    $scope.response = null;
    $scope.result = null;

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

        let operation = {
            url:$scope.currentAPI.url
        };
        //处理路径
        for(let i=0;i<apiParameters.length;i++){
            let apiParameter = apiParameters[i];
            if(apiParameter.position==="query"){
                operation.url = operation.url.replace("{"+apiParameter.name+"}",$scope.request[apiParameter.name]);
                delete $scope.request[apiParameter.name];
            }
        }
        let method = $scope.currentAPI.methods[0];
        if(method==="all"){
            method = "POST";
        }
        operation.method = method;
        if(method==="POST"||method==="PUT"||method==="PATCH"){
            if($scope.currentAPI.contentType.indexOf("multipart/form-data")>=0){
                let fd = new FormData();
                for(let prop in $scope.request){
                    fd.append(prop,document.getElementById(prop).files[0]);
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
        });
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
    }
});
