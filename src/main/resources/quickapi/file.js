let app = angular.module("app",[]);
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

app.controller("fileController",function($scope,$rootScope,$http,$httpParamSerializer,$location,$anchorScroll){
    $scope.apiDocument = apiDocument;

    $scope.getAPI = function(name){
        for(let i=0;i<$scope.apiDocument.apiControllerList.length;i++){
            let apiList = $scope.apiDocument.apiControllerList[i].apiList;
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
            }break;
            case "api":{
                $scope.currentAPI = val.api;
                $scope.setCurrentAPI($scope.currentAPI);
            }break;
        }
    };
    $scope.closeTab = function(key){
        delete $scope.tabMap[key];
    };

    //API搜索
    $scope.searchText = "";
    $scope.showEntity = function(className){
        if($scope.searchText===""){
            return true;
        }
        return className.indexOf($scope.searchText)>=0;
    };

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

    //显示API详情
    $scope.currentAPI = null;
    $scope.setCurrentAPI = function(api){
        if(null==api){
            return;
        }
        $scope.view = "api";
        $scope.tabMap[api.url+api.methods[0]] = {
            "name": api.name,
            "view": "api",
            "api": api
        };
        $scope.activeTabName = api.url+api.methods[0];

        $scope.currentAPI = api;
        $location.hash("top");
        $anchorScroll();
    };
});