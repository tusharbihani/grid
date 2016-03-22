import angular from 'angular';

import '../components/gu-lazy-gallery/gu-lazy-gallery';

export var gallery = angular.module('kahuna.search.gallery', [
    'gu.lazyGallery'
]);

// Global session-level state to remember the uploadTime of the first
// result in the last search.  This allows to always paginate the same
// set of results, as well as recovering the same set of results if
// navigating back to the same search.
// Note: I tried to do this using non-URL $stateParams and it was a
// rabbit-hole that doesn't seem to have any end. Hence this slightly
// horrid global state.
let lastSearchFirstResultTime;

gallery.controller('SearchGalleryCtrl', [
    '$rootScope',
    '$scope',
    '$state',
    '$stateParams',
    'mediaApi',

    function($rootScope,
             $scope,
             $state,
             $stateParams,
             mediaApi) {

        const ctrl = this;

        ctrl.images = [];

        function search({until, since, offset, length, orderBy} = {}) {
            // FIXME: Think of a way to not have to add a param in a million places to add it

            /*
             * @param `until` can have three values:
             *
             * - `null`      => Don't send over a date, which will default to `now()` on the server.
             *                  Used in `checkForNewImages` with no until in `stateParams` to search
             *                  for the new image count
             *
             * - `string`    => Override the use of `stateParams` or `lastSearchFirstResultTime`.
             *                  Used in `checkForNewImages` when a `stateParams.until` is set.
             *
             * - `undefined` => Default. We then use the `lastSearchFirstResultTime` if available to
             *                  make sure we aren't loading any new images into the result set and
             *                  `checkForNewImages` deals with that. If it's the first search, we
             *                  will use `stateParams.until` if available.
             */
            if (angular.isUndefined(until)) {
                until = lastSearchFirstResultTime || $stateParams.until;
            }
            if (angular.isUndefined(since)) {
                since = $stateParams.since;
            }
            if (angular.isUndefined(orderBy)) {
                orderBy = $stateParams.orderBy;
            }

            return mediaApi.search($stateParams.query, angular.extend({
                ids:        $stateParams.ids,
                archived:   $stateParams.archived,
                // The nonFree state param is the inverse of the free API param
                free:       $stateParams.nonFree === 'true' ? undefined: true,
                uploadedBy: $stateParams.uploadedBy,
                takenSince: $stateParams.takenSince,
                takenUntil: $stateParams.takenUntil,
                modifiedSince: $stateParams.modifiedSince,
                modifiedUntil: $stateParams.modifiedUntil,
                until:      until,
                since:      since,
                offset:     offset,
                length:     length,
                orderBy:    orderBy
            }));
        }

        ctrl.searched = search({length: 10, orderBy: 'newest'}).then(function(images) {
            ctrl.images = images.data;
            return images;
        });
    }
]);
