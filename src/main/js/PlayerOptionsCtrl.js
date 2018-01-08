com.digitald4.nbastats.PlayerOptionsCtrl = function(globalData, playerOptionService) {
	globalData.controlType = globalData.CONTROL_TYPE.DATE
	globalData.refresh = this.refresh.bind(this);
	this.globalData = globalData;
	this.playerOptionService = playerOptionService;
	this.refresh();
	this.sortProp = 'projection[\'RotoG Proj\']';
  this.reverse = true;
};

com.digitald4.nbastats.PlayerOptionsCtrl.prototype.refresh = function() {
  this.players = [];
  this.projectionMethods = [];
  var fantasyLeague = this.globalData.fantasyLeague;
	this.playerOptionService.list(this.globalData.getApiDate(), function(response) {
	  for (var method in response.result[0].fantasySiteInfo[fantasyLeague].projection) {
	    this.projectionMethods.push(method);
	  }
	  for (var i = 0; i < response.result.length; i++) {
	    var player = response.result[i];
	    this.players.push({
	      playerId: player.playerId,
	      name: player.name,
	      status: player.status,
	      team: player.team,
	      opponent: player.opponent,
	      position: player.fantasySiteInfo[fantasyLeague].position.join('/'),
	      cost: player.fantasySiteInfo[fantasyLeague].cost,
	      projection: player.fantasySiteInfo[fantasyLeague].projection,
	    });
	  }
	}.bind(this), notify);
};

com.digitald4.nbastats.PlayerOptionsCtrl.prototype.update = function(player, prop) {
  var index = this.players.indexOf(player);
  this.playerOptionService.update(player, [prop], function(player) {
    this.players[index] = player;
  }.bind(this), notify);
};

com.digitald4.nbastats.PlayerOptionsCtrl.prototype.processStats = function() {
  this.playerOptionService.processStats(this.globalData.getApiDate(), function(response) {
    console.log('Processing...');
  }.bind(this), notify);
};

com.digitald4.nbastats.PlayerOptionsCtrl.prototype.sortBy = function(prop) {
  if (this.sortProp == prop) {
    this.reverse = !this.reverse;
  } else {
	  this.sortProp = prop;
	  this.reverse = false;
	}
};