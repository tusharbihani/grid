import angular from 'angular';
import template from './gr-overlay-button.html!text';
import '../../util/rx';

export const overlayButton = angular.module('gr.overlayButton', ['util.rx']);

overlayButton.controller('GrOverlayButton', [function() {
        const ctrl = this;
        const overlay = ctrl.overlay;

        ctrl.showOverlay   = () => overlay.setHidden(false);
        ctrl.hideOverlay  = () => overlay.setHidden(true);
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
