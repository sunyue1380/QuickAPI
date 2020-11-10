app.register.component("menu",{
    //template和templateUrl二选一
    // template: ,
    templateUrl:"component/menu/menu.html",
    //是否包含其他组件
    transclude:false,
    //传值
    bindings: {
        //<父变子变,子变父不变 =双向改变
        "menuList":'<'
    },
    //控制器别名
    controllerAs:"vm",
    //引用父组件
    require:null,
    controller: function ($scope,$state) {
        $scope.currentMenu = "";
        /**路由*/
        $scope.goToPage = function(singleMenu){
            $state.go(singleMenu.state,singleMenu.params);
            $scope.currentMenu = singleMenu.name;
        };
    },
});