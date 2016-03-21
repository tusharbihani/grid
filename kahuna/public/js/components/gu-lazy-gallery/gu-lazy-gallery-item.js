import '../../util/rx';

export var lazyGalleryItem = angular.module('gu.lazyGalleryItem', [
    'util.rx'
]);

lazyGalleryItem.directive = ('guLazyGalleryItem', function() {
    return {
        restrict: 'A',
        require: '^guLazyGallery',
        transclude: true,
        link: function(scope, element, attrs, ctrl) {
            const item$ = observe$(scope, attrs.guLazyTableCell);

            console.log(item$);
        }
    };
});
