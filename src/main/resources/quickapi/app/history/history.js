app.register.controller("historyController", function ($scope, $rootScope, $state) {
    $scope.setCurrentAPI = function(api){
        $state.go("menu",{"api":api});
    }
});