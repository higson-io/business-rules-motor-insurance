(function () {
	'use strict';

	angular.module('hyperonDemoApp').directive('coverage', coverage);

	function coverage() {
		var directive = {
			restrict: 'E',
			templateUrl: 'components/variants/coverage/coverage.html',
			bindToController: true,
			controllerAs: 'ctrl',
			scope: {
				code: '<',
				name: '<',
				description: '<',
				limitLeft: '<',
				limitRight: '<',
				premium: '<'
			},
			controller: coverageCtrl
		};

		return directive;

		function coverageCtrl() {
		}
	}
})();
