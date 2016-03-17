import angular from 'angular';

export var gallery = angular.module('kahuna.search.gallery', []);

gallery.controller('SearchGalleryCtrl', [
    '$rootScope',
    '$scope',
    '$state',
    '$stateParams',

    function($rootScope,
             $scope,
             $state,
             $stateParams) {

        const ctrl = this;

        console.log(this);
    }
]);
