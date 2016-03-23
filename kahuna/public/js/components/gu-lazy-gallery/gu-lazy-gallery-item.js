import '../../util/rx';

export var lazyGalleryItem = angular.module('gu.lazyGalleryItem', [
    'util.rx'
]);

lazyGalleryItem.controller = ('GuLazyGalleryItemCtrl', [
    function() {
        const ctrl = this;
    }
]);

lazyGalleryItem.directive = ('guLazyGalleryItem', function() {
    return {
        restrict: 'A',
        require: '^guLazyGallery',
        transclude: true,
        template: '<ng-transclude></ng-transclude>',
        link: function(scope, element, attrs) {
            const item$ = observe$(scope, attrs.guLazyTableCell);
        }
    };
});
