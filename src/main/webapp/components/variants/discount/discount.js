(function () {
	'use strict';

	angular.module('hyperonDemoApp').directive('discount', discount);

	function discount() {
		var directive = {
			restrict: 'E',
			templateUrl: 'components/variants/discount/discount.html',
			bindToController: true,
			controllerAs: 'ctrl',
			scope: {
				priceBeforeDiscount: '<',
				name: '<',
				value: '<'
			},
			controller: discountCtrl
		};

		return directive;

		function discountCtrl() {
		}
	}
})();
