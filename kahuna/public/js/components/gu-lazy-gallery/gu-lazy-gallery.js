import angular from 'angular';
import Rx from 'rx';

import '../../util/rx';

export var lazyGallery = angular.module('gu.lazyGallery', [
    'util.rx'
]);

lazyGallery.controller('GuLazyGalleryCtrl', function() {
    let ctrl = this;
});

lazyGallery.directive('guLazyGallery', ['observe$',
    'observeCollection$', function(observe$, observeCollection$) {
    return {
        restrict: 'A',
        controller: 'GuLazyGalleryCtrl',
        transclude: true,
        template: '<p>This is a template</p> <ng-transclude></ng-transclude>',
        link: function (scope, element, attrs, ctrl) {
            // Map attributes as Observable streams
            const {
                guLazyGallery:             itemsAttr,
                guLazyTablePreloadedItems: preloadedItemsAttr
                } = attrs;

            const items$ = observeCollection$(scope, itemsAttr);

            console.log(items$);
        }
    };
}]);
