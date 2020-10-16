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

app.controller("indexController",function($scope,$rootScope,$http,$httpParamSerializer,$location,$anchorScroll){
    $scope.copyToClipBoard = function(value){
        let e = document.getElementById("textarea");
        e.value = value;
        e.select();
        document.execCommand("copy");
    };
    $scope.offline = false;
    //判断访问协议
    if(location.protocol.indexOf("file:")>=0){
        $scope.offline = true;
    }
    $scope.apiDocument = apiDocument;

    /**name=api.methods[0]+'_'+api.url*/
    $scope.getAPI = function(name){
        for(let i=0;i<$scope.apiDocument.apiControllerList.length;i++){
            let apiList = $scope.apiDocument.apiControllerList[i].apiList;
            for(let j=0;j<apiList.length;j++){
                let historyName = apiList[j].methods[0]+"_"+apiList[j].url;
                if(name.indexOf(historyName)>=0){
                    return apiList[j];
                }
            }
        }
        return "";
    };

    //处理历史变更记录
    for(let i=0;i<$scope.apiDocument.apiHistoryList.length;i++){
        let addList = $scope.apiDocument.apiHistoryList[i].addList;
        let modifyList = $scope.apiDocument.apiHistoryList[i].modifyList;
        for(let l=0;l<addList.length;l++){
            addList[l] = $scope.getAPI(addList[l]);
        }
        for(let l=0;l<modifyList.length;l++){
            modifyList[l] = $scope.getAPI(modifyList[l]);
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

    /**
     * 从本地存储中获取
     * */
    $scope.getFromLocalStorage = function(key,defaultValue){
        if(null!=localStorage.getItem(location.origin+location.pathname+"_"+key)){
            let value = JSON.parse(localStorage.getItem(location.origin+location.pathname+"_"+key));
            if(key=="#collectionList#"||key=="#lastUsed#") {
                for (let i = 0; i < value.length; i++) {
                    value[i] = $scope.getAPI(value[i]);
                }
            }
            return value;
        }
        return defaultValue;
    };
    /**
     * 保存到本地存储
     * */
    $scope.saveToLocalStorage = function(key,value){
        if(typeof(value)!="undefined"&&null!=value){
            if(key==="#collectionList#"||key==="#lastUsed#"){
                value = angular.copy(value);
                for(let i=0;i<value.length;i++){
                    value[i] = value[i].methods[0]+"_"+value[i].url;
                }
            }
            localStorage.setItem(location.origin+location.pathname+"_"+key,JSON.stringify(value));
        }
    };
    /**
     * 从本地存储中删除
     * */
    $scope.clearFromLocalStorage = function(key){
        if(null!=localStorage.getItem(location.origin+location.pathname+"_"+key)){
            localStorage.removeItem(location.origin+location.pathname+"_"+key);
        }
    };
    $scope.export = function(){
        let data = {};
        let prefix = location.origin+location.pathname+"_";
        for(let i=0;i<localStorage.length;i++){
            let key = localStorage.key(i);
            let index = key.indexOf(prefix);
            if(index>=0){
                data[key.substring(index+prefix.length)] = localStorage.getItem(key);
            }
        }
        $scope.copyToClipBoard(JSON.stringify(data));
        alert("数据已经复制到剪贴板!");
    };

    $scope.import = function(){
        let value = prompt("请输入导入数据");
        if(value==null||value==""){
            return;
        }
        try {
            let data = JSON.parse(value);
            for(let prop in data){
                localStorage.setItem(location.origin+location.pathname+"_"+prop,data[prop]);
            }
            alert("数据导入成功!");
        }catch (e) {
            alert("数据导入失败!");
            console.error(e);
        }
        location.reload();
    };

    //tab页设置
    $scope.activeTabName = "/quickapi/history";
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
                $scope.entity = val.entity;
                $scope.setCurrentEntity($scope.entity);
            };break;
            case "api":{
                $scope.currentAPI = val.api;
                $scope.setCurrentAPI($scope.currentAPI);
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

    //环境设置
    $scope.showSettings = function(){
        $scope.view = "settings";
        $scope.tabMap["/quickapi/settings"] = {
            "name": "文档设置",
            "view": "settings"
        };
        $scope.activeTabName = "/quickapi/settings";
    };

    $scope.settings = $scope.getFromLocalStorage("#settings#", {
        "showEntity":true,
        "showLastUsed":true,
        "lastUsedLength":5,
        "enableParameterCache":true,
    });
    $scope.$watch("settings",function(newValue,oldValue){
        $scope.saveToLocalStorage("#settings#",newValue);
    },true);

    //收藏功能
    $scope.collectionList = $scope.getFromLocalStorage("#collectionList#",[]);

    $scope.collect = function(api){
        let exist = $scope.collectionList.some(e=> {
            if (api.url === e.url) {
                return true;
            }
        });
        if(exist){
            return;
        }

        $scope.collectionList.push(api);
        $scope.saveToLocalStorage("#collectionList#",$scope.collectionList);
        api.hasCollect = true;
    };

    $scope.cancelCollect = function(api){
        for(let i=0;i<$scope.collectionList.length;i++){
            if($scope.collectionList[i]==api){
                $scope.collectionList.splice(i,1);
                break;
            }
        }
        api.hasCollect = false;
        $scope.saveToLocalStorage("#collectionList#",$scope.collectionList);
    };

    //最近使用
    $scope.lastUsed = $scope.getFromLocalStorage("#lastUsed#",[]);

    $scope.cleanCollectionList = function(){
        if(confirm("确认清空收藏记录吗?")){
            $scope.collectionList  = [];
            $scope.saveToLocalStorage("#collectionList#",$scope.collectionList);
        }
    };

    $scope.cleanHistory = function(){
        if(confirm("确认清空历史记录吗?")){
            $scope.lastUsed  = [];
            $scope.saveToLocalStorage("#lastUsed#",$scope.lastUsed);
        }
    };

    //实体类显示
    $scope.setCurrentEntity = function(entity){
        $scope.view = "entity";
        $scope.tabMap["/quickapi/entity/"+entity.className] = {
            "name": entity.simpleName,
            "view": "entity",
            "entity":entity
        };
        $scope.activeTabName = "/quickapi/entity/"+entity.className;

        $scope.entity = entity;
        $location.hash("top");
        $anchorScroll();
    };

    //API搜索
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

    //显示API详情
    $scope.currentAPI = null;
    $scope.api = {
        "url":"",
        "content":"",
        "request":null
    };
    $scope.$watch("api",function(newValue,oldValue){
        $scope.saveToLocalStorage(newValue.url,newValue);
    },true);
    $scope.initializeRequest = function(){
        let request = {};
        let apiParameters = $scope.currentAPI.apiParameters;
        for(let i=0;i<apiParameters.length;i++){
            request[apiParameters[i].name] = apiParameters[i].defaultValue;
            if("textarea"==apiParameters[i].requestType&&$scope.apiDocument.apiEntityMap.hasOwnProperty(apiParameters[i].type)){
                let data = JSON.parse($scope.apiDocument.apiEntityMap[apiParameters[i].type].instance);
                request[apiParameters[i].name] = JSON.stringify(data,null,4);
            }
        }
        $scope.api.request = request;
    };
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

        $scope.api = {
            "url":"",
            "content":"",
            "request":null
        };
        if($scope.settings.enableParameterCache){
            $scope.api = $scope.getFromLocalStorage(api.url,$scope.api);
        }
        $scope.api.url = api.url;
        if(null==$scope.api.request){
            $scope.initializeRequest();
        }

        $scope.currentAPI.hasCollect = false;
        for(let i=0;i<$scope.collectionList.length;i++){
            if($scope.collectionList[i]==api){
                $scope.currentAPI.hasCollect = true;
                break;
            }
        }

        $location.hash("top");
        $anchorScroll();
    };
    $scope.clearParameterCache = function(api){
        $scope.clearFromLocalStorage(api.url);
        $scope.initializeRequest();
    };
    $scope.saveAsParameter = function(){
        let name = prompt("请输入名称");
        if(null==name||name===""){
            return;
        }
        if(null==$scope.api.parameters){
            $scope.api.parameters = [];
        }
        //检查是否name重复
        for(let i=0;i<$scope.api.parameters.length;i++){
            if($scope.api.parameters[i].name===name){
                if(confirm("该名称已存在,是否覆盖?")){
                    $scope.api.parameters[i].request = $scope.api.request;
                }
                return;
            }
        }
        $scope.api.parameters.push({"name":name,"request":angular.copy($scope.api.request)});
    };
    $scope.deleteParameter = function(parameter){
        if(confirm("确认删除吗?")){
            for(let i=0;i<$scope.api.parameters.length;i++){
                if($scope.api.parameters[i].name==parameter.name){
                    $scope.api.parameters.splice(i,1);
                    break;
                }
            }
            $scope.initializeRequest();
        }
    };
    $scope.executeParameter = function(parameter){
        $scope.api.request = angular.copy(parameter.request);
        $scope.execute();
    };

    //处理search
    if(location.search!=""){
        let api = $scope.getAPI(location.search.substring(1));
        $scope.setCurrentAPI(api);
    }
    $scope.shareInterface = function(){
        $scope.copyToClipBoard(location.origin+location.pathname+"?"+$scope.currentAPI.methods[0]+"_"+$scope.currentAPI.url);
        alert("分享链接已复制到剪贴板!");
    };

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
    };
    $scope.response = null;
    let iframe = document.getElementById("iframe");
    $scope.iframe = document.all ? iframe.contentWindow.document : iframe.contentDocument;
    $scope.iframe.contentEditable = true;
    $scope.iframe.designMode = 'on';
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
                let value = $scope.api.request[apiParameter.name];
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
                operation.url = operation.url.replace("{"+apiParameter.name+"}",$scope.api.request[apiParameter.name]);
                delete $scope.api.request[apiParameter.name];
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
                for(let prop in $scope.api.request){
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
            }else if($scope.currentAPI.contentType.indexOf("application/json")>=0){
                for(let i=0;i<$scope.currentAPI.apiParameters.length;i++){
                    operation.data = $scope.api.request[$scope.currentAPI.apiParameters[i].name];
                }
            }else{
                let request = angular.copy($scope.api.request);
                let apiParameters = $scope.currentAPI.apiParameters;
                operation.data = "";
                //处理数组类型的参数
                for(let i=0;i<apiParameters.length;i++){
                    if(apiParameters[i].type.indexOf("[L")>=0||apiParameters[i].type.indexOf("<")>=0){
                        if(request[apiParameters[i].name]){
                            let values = request[apiParameters[i].name].split(",");
                            for(let j=0;j<values.length;j++){
                                operation.data += apiParameters[i].name + "=" + values[j]+"&";
                            }
                            delete request[apiParameters[i].name];
                        }
                    }
                }
                operation.data += $httpParamSerializer(request);
            }
        }else{
            operation.params = $scope.api.request;
        }
        operation.headers = {"Content-Type":$scope.currentAPI.contentType};
        if($scope.currentAPI.contentType.indexOf("multipart/form-data")>=0){
            operation.headers = {"Content-Type":undefined};
        }
        for(let prop in $rootScope.headers){
            operation.headers[prop] = $rootScope.headers[prop];
        }
        $scope.loading = true;
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
        },function(error){
            $scope.response = error;
            $scope.responseJSON = JSON.stringify(error.data,null,4);
        }).finally(function(){
            $scope.iframe.open();
            $scope.iframe.write($scope.response.data);
            $scope.iframe.close();

            let endTime = new Date().getTime();
            $scope.consumeTime = (endTime-startTime)+"ms";
            $scope.loading = false;

            if($scope.lastUsed.length>$scope.settings.lastUsedLength){
                $scope.lastUsed.shift();
            }
            //判断是否有重复
            let exist = $scope.lastUsed.some(e=> {
                if (api.url === e.url) {
                    return true;
                }
            });
            if(exist){
                return;
            }
            if(!exist){
                $scope.lastUsed.unshift($scope.currentAPI);
                $scope.saveToLocalStorage("#lastUsed#",$scope.lastUsed);
            }
        });
    };
});