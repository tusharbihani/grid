import angular from 'angular';
import Rx from 'rx';

import '../../util/rx';

export var lazyGallery = angular.module('gu.lazyGallery', [
    'util.rx'
]);

lazyGallery.controller('GuLazyGalleryCtrl', function() {
    let ctrl = this;

    ctrl.getCurrentItem = function() {
        console.log("CURRENT ITEM");
    }
});

lazyGallery.directive('guLazyGallery', ['observe$',
    'observeCollection$', function(observe$, observeCollection$) {
    return {
        restrict: 'A',
        controller: 'GuLazyGalleryCtrl',
        transclude: true,
        template: '<ng-transclude></ng-transclude>',
        link: function (scope, element, attrs, ctrl) {
            // Map attributes as Observable streams
            const {
                guLazyGallery:             itemsAttr,
                guLazyTablePreloadedItems: preloadedItemsAttr
                } = attrs;

            const items$ = observeCollection$(scope, itemsAttr),
                  preloadedItem$ = observeCollection$(scope, preloadedItemsAttr);
        }
    };
}]);
