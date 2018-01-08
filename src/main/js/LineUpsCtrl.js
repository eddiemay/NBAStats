com.digitald4.nbastats.LineUpsCtrl = function(globalData, lineUpService) {
  globalData.controlType = globalData.CONTROL_TYPE.DATE
	globalData.refresh = this.refresh.bind(this);
	this.globalData = globalData;
	this.lineUpService = lineUpService;
	this.refresh();
};

com.digitald4.nbastats.LineUpsCtrl.prototype.refresh = function() {
	this.lineUpService.list(this.globalData.getApiDate(), this.globalData.fantasyLeague, this.globalData.projectionMethod,
	    function(response) {
	  this.lineUps = response.result;
	}.bind(this), notify);
};