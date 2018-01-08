com.digitald4.nbastats.router = function($routeProvider) {
	$routeProvider
		.when('/', {
				templateUrl: 'js/html/defview.html'
		}).when('/players', {
				controller: com.digitald4.nbastats.PlayersCtrl,
				controllerAs: 'playersCtrl',
				templateUrl: 'js/html/players.html'
		}).when('/playerOptions', {
				controller: com.digitald4.nbastats.PlayerOptionsCtrl,
				controllerAs: 'playerOptionsCtrl',
				templateUrl: 'js/html/playerOptions.html'
		}).when('/lineups', {
				controller: com.digitald4.nbastats.LineUpsCtrl,
				controllerAs: 'lineUpsCtrl',
				templateUrl: 'js/html/lineups.html'
		}).otherwise({ redirectTo: '/'});
};
