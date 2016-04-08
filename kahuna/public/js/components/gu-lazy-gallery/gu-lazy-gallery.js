import angular from 'angular';
import Rx from 'rx';

import '../../util/rx';
import './gu-lazy-gallery-item';
import './gu-lazy-gallery-control';

export var lazyGallery = angular.module('gu.lazyGallery', [
    'util.rx',
    'gu.lazyGalleryItem',
    'gu.lazyGalleryControl'
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
