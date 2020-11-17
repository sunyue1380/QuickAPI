app.register.controller("apiController", function ($scope, $rootScope, $state, $storageService, $http, $location, $anchorScroll) {
    $location.hash("top");
    $anchorScroll();

    $scope.api = $state.params.api;

    $scope.storageService = $storageService;
    //获取请求参数
    $scope.getRequest = function(){
        let request = {};
        for(let i=0;i<$scope.api.apiParameters.length;i++){
            let apiParameter = $scope.api.apiParameters[i];
            request[apiParameter.name] = apiParameter.defaultValue;
            if("textarea"===apiParameter.requestType&&$rootScope.apiDocument.apiEntityMap.hasOwnProperty(apiParameter.type)){
                let data = JSON.parse($rootScope.apiDocument.apiEntityMap[apiParameter.type].instance);
                request[apiParameter.name] = JSON.stringify(data,null,4);
            }
        }
        return request;
    };

    $scope.shareInterface = function(){
        $scope.copyToClipBoard(location.origin+location.pathname+"?"+$scope.currentAPI.methods[0]+"_"+$scope.currentAPI.url);
        alert("分享链接已复制到剪贴板!");
    };

    //记录该api需求说明
    $scope.apiStorage = $storageService["API_"+$scope.api.url];
    if(!$scope.apiStorage){
        $scope.apiStorage = {
            "collect":false,
            "request":$scope.getRequest(),
            "api":$scope.api,
            "callbackFunction":null
        };
    }
    $scope.$watch("apiStorage",function(newValue,oldValue){
        $storageService["API_"+$scope.api.url] = newValue;
    },true);

    //顶部按钮栏
    $scope.toggleCollect = function(){
        if($scope.apiStorage.collect){
            //取消收藏
            $scope.apiStorage.collect = false;
            for(let i=0;i<$rootScope.collectionList.length;i++){
                if($rootScope.collectionList[i].url==$scope.api.url){
                    $rootScope.collectionList.splice(i,1);
                    break;
                }
            }
            $storageService.collectionList = $rootScope.collectionList;
        }else{
            //收藏
            $scope.apiStorage.collect = true;
            let exist = $rootScope.collectionList.some(e=> {
                if ($scope.api.url === e.url) {
                    return true;
                }
            });
            if(!exist){
                $rootScope.collectionList.push(angular.copy($scope.api));
            }
            $storageService.collectionList = $rootScope.collectionList;
        }
    };
    $scope.showAPIModal = function(){
        $scope.apiDataOption.show = true;
    };

    /**API相关信息*/
    $scope.apiDataFields = [
        {
            "label":"API信息",
            "type":"textarea",
            "model":"api",
        },
        {
            "label":"存储信息",
            "type":"textarea",
            "model":"storage",
        },
    ];
    $scope.apiData = {
        "api":JSON.stringify($scope.api,null,4),
        "storage":JSON.stringify($scope.apiStorage,null,4)
    };
    $scope.apiDataOption = {
        "show":false,
        "title":"API相关信息"
    };

    /**请求参数字段*/
    $scope.requestParameterFields = [
        {
            "key": "name",
            "value": "参数名"
        },
        {
            "key": "type",
            "value": "参数类型"
        },
        {
            "key": "position",
            "value": "参数位置"
        },
        {
            "key": "description",
            "value": "描述",
            "type":"format",
            "formatter":function(data){
                return data.description;
            }
        },
        {
            "key": "required",
            "value": "是否必须",
            "type":"tags",
            "tags":[
                {
                    "class":"is-info",
                    "show":function(data){
                        return data.required;
                    },
                    "name":"必须"
                }
            ]
        },
        {
            "key": "defaultValue",
            "value": "默认值"
        },
        {
            "key": "requestValue",
            "value": "参数值",
            "type": "form",
            "formType":function(data){
                return data.requestType;
            }
        }
    ];

    //保存请求参数列表
    $scope.saveParameterList = $scope.apiStorage.saveParameterList;
    $scope.saveAsParameter = function(){
        if(!$scope.apiStorage.saveParameterList){
            $scope.apiStorage.saveParameterList = [];
        }
        let value = prompt("请输入请求参数名称");
        if(null==value||value===""){
            return;
        }
        let saveParameter = {
            "name":value,
            "executeOnRefresh":false,
            "request":angular.copy($scope.apiStorage.request)
        };
        let exist = $scope.apiStorage.saveParameterList.some(e=> {
            if (saveParameter.name === e.name) {
                return true;
            }
        });
        if(!exist){
            $scope.apiStorage.saveParameterList.push(saveParameter);
        }
    };

    $scope.saveParameterFields = [
        {
            "key": "name",
            "value": "名称",
        },
        {
            "key":"operation",
            "value":"操作",
            "type":"buttons",
            "buttons":[
                {
                    "class": "is-primary",
                    "name": "查看",
                    "click": function (data) {
                        $scope.apiStorage.request = data.request;
                    }
                },
                {
                    "class":"is-info",
                    "name":"执行",
                    "click":function(data){
                        $scope.execute($scope.api,data.request);
                    }
                },
                {
                    "class":"is-danger",
                    "name":"删除",
                    "click":function(data,index){
                        if(confirm("确认删除吗?")){
                            $scope.apiStorage.saveParameterList.splice(index,1);
                        }
                    }
                },
                {
                    "class":"is-link",
                    "show":function(data){
                        return !data["executeOnRefresh"];
                    },
                    "name":"刷新时执行",
                    "click":function(data){
                        data["executeOnRefresh"] = true;
                    }
                },
                {
                    "class":"is-danger",
                    "show":function(data){
                        return data["executeOnRefresh"];
                    },
                    "name":"取消刷新时执行",
                    "click":function(data){
                        data["executeOnRefresh"] = false;
                    }
                }
            ]
        }
    ];

    /**重置请求参数*/
    $scope.resetParameter = function(){
        $scope.apiStorage.request = $scope.getRequest();
    };

    /**回调函数*/
    $scope.initCallbackFunction = function(){
        $scope.apiStorage.callbackFunction =
            "/**\n" +
            "     * API执行结束后回调函数\n" +
            "     * @param data API返回结果\n" +
            "     * @param globalHeaders 全局头部数组\n" +
            "     */\n" +
            "    function callback(data,globalHeaders){\n" +
            "        /**console.log(data);*/\n" +
            "        /**globalHeaders.push*({'key':'key','value':'value','remark':'example'})*/\n" +
            "    }";
    };
    if(typeof($scope.apiStorage.callbackFunction)=="undefined"||$scope.apiStorage.callbackFunction==null){
        $scope.initCallbackFunction();
    }
    $scope.resetCallbackFunction = function(){
        if(confirm("确认重置回调函数吗?")){
            $scope.initCallbackFunction();
        }
    };

    /**异常表信息*/
    $scope.exceptionFields = [
        {
            "key":"className",
            "value":"异常类"
        },
        {
            "key":"description",
            "value":"说明"
        },
    ];

    //计算请求耗费时间
    $scope.consumeTime = "";
    //Body显示样式
    $scope.responseView = {
        "view": "raw",
        "type":"object",
        "keys": []
    };
    $scope.changeResponseView = function(view){
        $scope.responseView.view = view;
        if(view==="html"){
            let iframe = document.getElementById("iframe");
            $scope.iframe = document.all ? iframe.contentWindow.document : iframe.contentDocument;
            $scope.iframe.contentEditable = true;
            $scope.iframe.designMode = 'on';
        }
    };
    $scope.response = null;
    /**
     * 执行请求
     * @param api 请求api信息
     * @param request 请求参数信息
     * */
    $scope.execute = function(api,request){
        //检查必填项
        {
            for(let i=0;i<api.apiParameters.length;i++){
                let apiParameter = api.apiParameters[i];
                if(apiParameter.requestType==="file"){
                    if(apiParameter.required&&document.getElementById(apiParameter.name).files.length===0){
                        alert("请填写必填项:"+apiParameter.name);
                        return;
                    }
                }else{
                    let value = request[apiParameter.name];
                    if(apiParameter.required&&(typeof(value)=="undefined"||value==="")){
                        alert("请填写必填项:"+apiParameter.name);
                        return;
                    }
                }
            }
        }

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
                    if(null!=document.getElementById(prop)){
                        let files = document.getElementById(prop).files;
                        for(let i=0;i<files.length;i++){
                            fd.append(prop,files[i]);
                        }
                    }else{
                        fd.append(prop,$scope.api.request[prop]);
                    }
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
        let startTime = new Date().getTime();
        $http(operation).then(function(response){
            $scope.response = response;
            $scope.responseJSON = JSON.stringify(response.data,null,4);
            if(Object.prototype.toString.call(response.data)=='[object Object]'){
                $scope.responseView.type = "object";
                $scope.responseView.keys = Object.keys($scope.response.data);
            }else if(Object.prototype.toString.call(response.data)=='[object Array]'){
                $scope.responseView.type = "array";
                $scope.responseView.keys = [];
                if(response.data.length>0){
                    $scope.responseView.keys = Object.keys($scope.response.data[0]);
                }
            }
            //执行回调函数
            try {
                let fn = new Function("data","globalHeaders",$scope.apiStorage.callbackFunction.substring($scope.apiStorage.callbackFunction.indexOf("{")+1,$scope.apiStorage.callbackFunction.lastIndexOf("}")));
                let settings = $storageService.settings;
                fn(response.data,settings.globalHeaders);
                $storageService.settings = settings;
            }catch (e) {
                console.error(e);
            }
        },function(error){
            $scope.response = error;
            $scope.responseJSON = JSON.stringify(error.data,null,4);
        }).finally(function(){
            if($scope.responseView.view==="html"){
                $scope.iframe.open();
                $scope.iframe.write($scope.response.data);
                $scope.iframe.close();
            }

            let endTime = new Date().getTime();
            $scope.consumeTime = (endTime-startTime)+"ms";

            let exist = $rootScope.recentUsedList.some(e=> {
                if (operation.url === e.url) {
                    return true;
                }
            });
            if(!exist){
                if($rootScope.recentUsedList.length>5){
                    $rootScope.recentUsedList.shift();
                }
                $rootScope.recentUsedList.unshift(angular.copy(api));
                $storageService.recentUsedList = $rootScope.recentUsedList;
            }
        });
    };
});