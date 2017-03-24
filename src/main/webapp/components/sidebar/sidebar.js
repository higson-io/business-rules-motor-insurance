(function () {
	'use strict';

	angular.module('hyperonDemoApp').directive('sidebar', sidebar);

	function sidebar() {
		var directive = {
			restrict: 'E',
			templateUrl: 'components/sidebar/sidebar.html',
			bindToController: true,
			controllerAs: 'ctrl',
			scope: {},
			controller: sidebarCtrl
		};

		return directive;

		function sidebarCtrl() {
			var ctrl = this;

			ctrl.sidebarHidden = false;

			ctrl.toggleSidebar = toggleSidebar;

			function toggleSidebar() {
				ctrl.sidebarHidden = !ctrl.sidebarHidden;
			}
		}
	}
})();
