app.register.component("formModal",{
    //template和templateUrl二选一
    // template: ,
    templateUrl:"component/formModal/formModal.html",
    //是否包含其他组件
    transclude:false,
    //传值
    bindings: {
        //<父变子变,子变父不变 =双向改变
        "option":'=',
        "data":'=',
        "fields":'<'
    },
    //控制器别名
    controllerAs:"vm",
    //引用父组件
    require:null,
    controller: function ($scope) {
        this.$onInit = function(){
            $scope.option = this.option;
        };

        $scope.closeModal = function(){
            $scope.vm.option.show = false;
        }
    }
});