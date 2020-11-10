app.register.directive('convertToNumber', function() {
    return {
        require: 'ngModel',
        link: function(scope, element, attrs, ngModel) {
            ngModel.$parsers.push(function(val) {
                return val != null ? parseInt(val, 10) : null;
            });
            ngModel.$formatters.push(function(val) {
                return val != null ? '' + val : null;
            });
        }
    };
});

app.register.component("pagination",{
    //template和templateUrl二选一
    // template: ,
    templateUrl:"component/pagination/pagination.html",
    //是否包含其他组件
    transclude:false,
    //传值
    bindings: {
        "pageVo": '=',
        "getList": '<'
    },
    //控制器别名
    controllerAs:"vm",
    //引用父组件
    require:null,
    controller: function ($scope) {
        this.$onInit = function(){
            this.pageVo.pageSize = this.pageVo.pageSize?this.pageVo.pageSize:10;
            $scope.pageNumber = this.pageVo.currentPage;
        };
        $scope.toPage = function (pageNumber) {
            if (pageNumber === 0 || pageNumber === ($scope.vm.pageVo.totalPage + 1)) {
                return;
            }
            if (typeof(pageNumber) == "undefined") {
                pageNumber = $scope.pageNumber;
            }
            if (pageNumber >= 0 && pageNumber <= $scope.vm.pageVo.totalPage) {
                $scope.vm.getList(pageNumber);
            } else {
                $scope.vm.pageVo.currentPage = 1;
            }
        };
    },
});