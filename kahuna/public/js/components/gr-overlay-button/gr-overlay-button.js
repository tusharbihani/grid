import angular from 'angular';
import template from './gr-overlay-button.html!text';
import '../../util/rx';

export const overlayButton = angular.module('gr.overlayButton', ['util.rx']);

overlayButton.controller('GrOverlayButton', ['$scope', 'inject$', function($scope, inject$) {
        const ctrl = this;
        const overlay = ctrl.overlay;

        ctrl.showOverlay   = () => overlay.setHidden(false);
        ctrl.hideOverlay  = () => overlay.setHidden(true);

        inject$($scope, overlay.state$, ctrl, 'state');
    }
]);

overlayButton.directive('grOverlayButton', [function() {
    return {
        restrict: 'E',
        template: template,
        bindToController: true,
        controller: 'GrOverlayButton',
        controllerAs: 'ctrl',
        scope: {
            overlay: '=grOverlay',
            name: '@grName'
        }
    };
}]);
