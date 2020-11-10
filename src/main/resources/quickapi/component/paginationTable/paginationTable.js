app.register.component("paginationTable",{
    //template和templateUrl二选一
    // template: ,
    templateUrl:"component/paginationTable/paginationTable.html",
    //是否保留内部html标签
    transclude:true,
    //传值
    bindings: {
        "fields":'<',
        "pageVo": '=',
        "getList": '<',
        "track":'<'
    },
    //控制器别名
    controllerAs:"vm",
    //引用父组件
    require: null,
    controller: function () {

    }
});