(function () {
	'use strict';

	angular.module('hyperonDemoApp').directive('variants', variants);

	function variants() {
		var directive = {
			restrict: 'E',
			templateUrl: 'components/variants/variants.html',
			bindToController: true,
			controllerAs: 'ctrl',
			scope: {},
			controller: variantsCtrl
		};

		return directive;

		function variantsCtrl($scope, VariantService) {
			var ctrl = this;

			init();

			function init() {
				$scope.$on('reloadQuote', function () {
					reloadQuote();
				});
				reloadQuote();
			}

			function reloadQuote() {
				VariantService.getQuote()
					.then(function (response) {
						ctrl.variants = response.data.options;
						ctrl.errorOccured = false;
					}, function (error) {
						ctrl.errorOccured = true;
						ctrl.errorMessage = error.data.message;
					});
			}
		}
	}
})();
