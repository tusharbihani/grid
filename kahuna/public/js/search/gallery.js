import angular from 'angular';
import Rx from 'rx';
import * as querySyntax from '../search-query/query-syntax';
import moment from 'moment';

import '../services/scroll-position';
import '../services/panel';
import '../util/async';
import '../util/rx';
import '../util/seq';
import '../components/gu-lazy-table/gu-lazy-table';
import '../components/gu-lazy-table-shortcuts/gu-lazy-table-shortcuts';
import '../components/gr-archiver/gr-archiver';
import '../components/gr-delete-image/gr-delete-image';
import '../components/gr-downloader/gr-downloader';
import '../components/gr-panel-button/gr-panel-button';

export var gallery = angular.module('kahuna.search.gallery', [
    'kahuna.services.scroll-position',
    'kahuna.services.panel',
    'util.async',
    'util.rx',
    'util.seq',
    'gu.lazyTable',
    'gu.lazyTableShortcuts',
    'gr.archiver',
    'gr.downloader',
    'gr.deleteImage',
    'gr.panelButton'
]);

results.controller('SearchResultsCtrl', [
    '$rootScope',
    '$scope',
    '$state',
    '$stateParams',
    '$window',
    '$timeout',
    '$log',
    '$q',
    'inject$',
    'delay',
    'onNextEvent',
    'scrollPosition',
    'mediaApi',
    'selection',
    'selectedImages$',
    'results',
    'panels',
    'range',
    'isReloadingPreviousSearch',

    function($rootScope,
             $scope,
             $state,
             $stateParams,
             $window,
             $timeout,
             $log,
             $q,
             inject$,
             delay,
             onNextEvent,
             scrollPosition,
             mediaApi,
             selection,
             selectedImages$,
             results,
             panels,
             range,
             isReloadingPreviousSearch) {

        const ctrl = this;

        console.log(this);
    }
]);
