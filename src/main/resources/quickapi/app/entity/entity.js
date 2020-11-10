app.register.controller("entityController", function ($scope, $rootScope, $state) {
    $scope.entity = $state.params.entity;
});