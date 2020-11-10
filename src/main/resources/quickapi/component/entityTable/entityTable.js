app.register.component("entityTable",{
    //template和templateUrl二选一
    // template: ,
    templateUrl:"component/entityTable/entityTable.html",
    //是否包含其他组件
    transclude:false,
    //传值
    bindings: {
        //<父变子变,子变父不变 =双向改变
        "entity":'<',
    },
    //控制器别名
    controllerAs:"vm",
    //引用父组件
    require:null,
    controller: function ($scope) {

    }
});