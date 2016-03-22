import angular from 'angular';
import Rx from 'rx';
import 'rx-dom';

import './gr-overlay.css!';

import {overlayService} from '../../services/overlay';

export const overlay = angular.module('gr.overlay', [
    overlayService.name
]);

overlay.directive('grOverlay', [function() {
        return {
            restrict: 'E',
            replace: true,
            transclude: true,
            scope: {
                panel: '=grOverlay'
            },
            template: `<div class="gr-overlay"
                ng:class="{ 'gr-overlay--hidden': state.hidden }">
                <div class="gr-overlay__content">
                    <ng:transclude></ng:transclude>
                </div>
            </div>`
        };
    }
]);
