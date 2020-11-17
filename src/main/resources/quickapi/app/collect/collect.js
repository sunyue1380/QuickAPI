app.register.controller("collectController", function ($scope, $rootScope, $state) {
    $scope.collectFields = [
        {
            "key":"methods",
            "value":"请求方法",
            "type":"format",
            "formatter":function(data){
                return "<span class='tag is-primary'>"+data.methods[0]+"</span>";
            }
        },
        {
            "key":"url",
            "value":"url",
            "type":"format",
            "tips":function(data){
                return data.url
            },
            "formatter":function(data){
                return data.url.substring(0,30);
            }
        },
        {
            "key":"description",
            "value":"描述",
            "type":"format",
            "tips":function(data){
                return data.description
            },
            "formatter":function(data){
                return data.description.substring(0,30);
            }
        },
        {
            "key":"operation",
            "value":"操作",
            "type":"buttons",
            "buttons":[
                {
                    "name":"查看详情",
                    "class":"is-primary",
                    "click":function(data){
                        $state.go("menu",{"name":"api","api":data});
                    }
                },
                {
                    "name":"取消收藏",
                    "class":"is-danger",
                    "click":function(data,index){
                        if(confirm("确认取消收藏吗?")){
                            $rootScope.collectionList.splice(index,1);
                        }
                    }
                }
            ]
        },
    ];

    $scope.buttonList = [
        {
            "name":"清空收藏列表",
            "click":function(){
                if(confirm("确认清空收藏列表吗?")){
                    $rootScope.collectionList = [];
                    $storageService.collectionList = [];
                }
            }
        }
    ];
});