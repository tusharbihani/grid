import angular from 'angular';
import Rx from 'rx';

import '../../util/rx';

export var lazyGallery = angular.module('gu.lazyGallery', [
    'util.rx'
]);

lazyGallery.controller('GuLazyGalleryCtrl', function() {
    let ctrl = this;
});

lazyGallery.directive('guLazyGallery', ['observeCollection$', function(observeCollection$) {
    return {
        restrict: 'E',
        controller: 'GuLazyGalleryCtrl',
        template: './gu-lazy-gallery.html',
        link: function (scope, element, attrs, ctrl) {


            console.log(attrs);
        }
    };
}]);
