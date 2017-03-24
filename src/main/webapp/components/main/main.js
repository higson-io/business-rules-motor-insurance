(function () {
	'use strict';

	angular.module('hyperonDemoApp').directive('main', main);

	function main() {
		var directive = {
			restrict: 'E',
			templateUrl: 'components/main/main.html',
			bindToController: true,
			controllerAs: 'ctrl',
			scope: {},
			controller: mainCtrl
		};

		return directive;

		function mainCtrl($scope) {
			var ctrl = this;

			$scope.$on('recalculate', function (event, data) {
				$scope.$broadcast('reloadQuote', data);
			});
		}
	}
})();
