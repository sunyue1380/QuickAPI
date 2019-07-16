let app = angular.module("app",[]);


app.controller("indexController",function($scope,$http,$httpParamSerializer,$location,$anchorScroll){
    $scope.apiDocument = {};
    $scope.apiControllerList = [];
    $http.get(location.pathname.substring(0,location.pathname.lastIndexOf("/"))+"/api.json").then(function(response){
        $scope.apiDocument = response.data;
        $scope.apiControllerList = $scope.apiDocument.apiControllerList;
    });

    $scope.currentAPI = null;
    $scope.setCurrentAPI = function(api){
        $scope.currentAPI = api;
        $scope.request = {};
        let apiParameters = $scope.currentAPI.apiParameters;
        for(let i=0;i<apiParameters.length;i++){
            $scope.request[apiParameters[i].name] = apiParameters[i].defaultValue;
        }
        $scope.response = null;
        $scope.result = null;

        $location.hash("top");
        $anchorScroll();
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
            url:$scope.currentAPI.url,
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
        $scope.loading = true;
        $http(operation).then(function(response){
            $scope.response = response;
            $scope.responseJSON = JSON.stringify(response.data,null,4);
        },function(error){
            $scope.response = error;
        }).finally(function(){
            $scope.loading = false;
        });
    };
});