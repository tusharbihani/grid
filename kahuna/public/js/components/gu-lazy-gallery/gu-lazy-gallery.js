import angular from 'angular';
import Rx from 'rx';

import '../../util/rx';
import template from './gu-lazy-gallery.html!text';

export var lazyGallery = angular.module('gu.lazyGallery', [
    'util.rx'
]);

lazyGallery.controller('GuLazyGalleryCtrl', ['$scope', function($scope) {
    let ctrl = this,
        pos = 0;

    // Set gallery
    $scope.gallery = {};

    function setTransform() {
        $scope.gallery[0].style.transform = 'translate3d(' + (-pos * $scope.gallery[0].offsetWidth) + 'px,0,0)';
    }

    ctrl.previousItem = function() {
        pos = Math.max(pos - 1, 0);
        setTransform();
    };

    ctrl.nextItem = function() {
        pos = Math.min(pos + 1, $scope.gallery[0].children.length - 1);
        setTransform();
    };
}]);

lazyGallery.directive('guLazyGallery', function() {
    return {
        restrict: 'E',
        template: template,
        controller: 'GuLazyGalleryCtrl',
        controllerAs: 'ctrl',
        scope: {
            galleryItems: '=guLazyGalleryItems'
        }
    };
});

lazyGallery.directive('guLazyGalleryList', function() {
    return {
        restrict: 'A',
        scope: {
            gallery: '=guLazyGalleryList'
        },
        link: function(scope, element) {
            //Bind list items to scope.

            scope.gallery = element;
        }
    };
});
