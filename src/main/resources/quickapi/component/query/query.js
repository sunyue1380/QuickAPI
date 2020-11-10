app.register.component("query",{
    //template和templateUrl二选一
    // template: ,
    templateUrl:"component/query/query.html",
    //是否保留内部html标签
    transclude:false,
    //传值
    bindings: {
        /**查询条件对象*/
        data:"=",
        /**查询字段信息*/
        fields:"=",
        type:"<",
        /**每列所占宽度(1-12)*/
        col:"<"
    },
    //控制器别名
    controllerAs:"vm",
    //引用父组件
    require: null,
    controller: function ($scope) {
        this.$onInit = function(){
            if(!this.type){
                this.type = "horizontal";
            }
            if(!this.col){
                this.col = 4;
            }
        };
    }
});