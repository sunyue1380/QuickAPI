app.register.filter('trustAsHtml', function ($sce) {
    return function (value) {
        return $sce.trustAsHtml(value);
    };
});

app.register.component("bulmaTable",{
    //template和templateUrl二选一
    // template: ,
    templateUrl:"component/bulmaTable/bulmaTable.html",
    //是否包含其他组件
    transclude:false,
    //传值
    bindings: {
        //<父变子变,子变父不变 =双向改变
        "list":'<',
        "fields":'<',
        "track":'<',
        "formModel":'<',
    },
    //控制器别名
    controllerAs:"vm",
    //引用父组件
    require:null,
    controller: function ($scope) {
        this.$onInit = function(){
            if(typeof(this.formModel)=="undefined"){
                this.formModel = {};
            }
        }
    }
});