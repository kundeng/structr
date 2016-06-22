var Graphbrowser = Graphbrowser || {};
Graphbrowser.Control = Graphbrowser.Control || {};

var animating = animating || undefined;

(function() {
	'use strict';

	var _s , _callbacks, _sigmaSettings, _sigmaContainer, _dragListener, _activeState, _lasso, _keyboard, _select;
	var _hasDragged = false, _shiftKey = false, _ctrlKey = false, _altKey = false;
	var _keyupHandlers = [],  _keydownHandlers = [];

	Graphbrowser.Control.SigmaControl = function(callbacks, sigmaSettings, sigmaContainer){
		var self = this;
		self.name = 'SigmaControl';
		_callbacks = callbacks;
		_sigmaSettings = sigmaSettings;
		_sigmaContainer = sigmaContainer;
	};

	Graphbrowser.Control.SigmaControl.prototype.init = function() {
		var self = this;
		var sigmaContainer, sigmaSettings;

		_sigmaContainer && typeof _sigmaContainer === 'string' ? 
			sigmaContainer = _sigmaContainer :
				sigmaContainer = "graph-container";

		_sigmaSettings !== undefined ? sigmaSettings = _sigmaSettings : 
		sigmaSettings = {
			immutable: false,
			minNodeSize: 1,
			maxNodeSize: 10,
			borderSize: 4,
			defaultNodeBorderColor: '#a5a5a5',
			singleHover: true,
			doubleClickEnabled: false,
			minEdgeSize: 4,
			maxEdgeSize: 4,
			enableEdgeHovering: true,
			edgeHoverColor: 'default',
			edgeHoverSizeRatio: 1.3,
			edgeHoverExtremities: true,
			defaultEdgeColor: '#999',
			defaultEdgeHoverColor: '#888',
			minArrowSize: 4,
			maxArrowSize: 8,
			labelSize: 'proportional',
			labelSizeRatio: 1,
			labelAlignment: 'right',
			nodeHaloColor: 'rgba(236, 81, 72, 0.2)',
			nodeHaloSize: 20,
		};

		sigma.renderers.def = sigma.renderers.canvas;
		_s = new sigma({
			container: sigmaContainer,
			settings: sigmaSettings
		});

		_activeState = sigma.plugins.activeState(_s);

		_keyboard = sigma.plugins.keyboard(_s, _s.renderers[0]);

		_select = sigma.plugins.select(_s, _activeState);
		_select.bindKeyboard(_keyboard);
		
		_dragListener = new sigma.plugins.dragNodes(_s, _s.renderers[0], _activeState);

		_lasso = new sigma.plugins.lasso(_s, _s.renderers[0], {
			'strokeStyle': 'rgb(236, 81, 72)',
			'lineWidth': 2,
			'fillWhileDrawing': true,
			'fillStyle': 'rgba(236, 81, 72, 0.2)',
			'cursor': 'crosshair'
		});

		_select.bindLasso(_lasso);

		_keyboard.bind('32+83', function() {
			self.toggleLasso();
		});

		self.activate();

		$(window).keyup(function(e) {
			switch(e.which){
				case 16:
					_shiftKey = false;
					break;
				case 17:
					_ctrlKey = false;
					break;
				case 18:
					_altKey = false;
					break;
				default:
					return false;
			}
			var handler;
			var noKey = _shiftKey || _altKey || _ctrlKey;
			for(var i = 0; i < _keyupHandlers.length; i++){
				_keyupHandlers[i]({shiftKey: _shiftKey, ctrlKey: _ctrlKey, altKey: _altKey, noKey: !noKey});
			}
		});

		$(window).on('keydown', function(e) {
			// This hack prevents FF from closing WS connections on ESC
			if (e.keyCode === 27) {
				e.preventDefault();
			}
			switch(e.which){
				case 16:
					_shiftKey = true;
					break;
				case 17:
					_ctrlKey = true;
					break;
				case 18:	
					_altKey = true;
					break;
			}
			var handler;
			var noKey = _shiftKey || _altKey || _ctrlKey;
			for(var i = 0; i < _keydownHandlers.length; i++){
				_keydownHandlers[i]({shiftKey: _shiftKey, ctrlKey: _ctrlKey, altKey: _altKey, noKey: !noKey});
			}
		});

		//for other modules who want to listen to sigmaevents or manipulate the behaviour of the plugins
		_callbacks.sigmaCtrl = {};
		_callbacks.sigmaPlugins = {};
		_callbacks.sigmaCtrl.unbindSigmaEvent = self.unbindEvent;
		_callbacks.sigmaCtrl.bindSigmaEvent = self.bindEvent;
		_callbacks.sigmaPlugins.activeState = _activeState;
		_callbacks.sigmaPlugins.keyboard = _keyboard;
		_callbacks.sigmaPlugins.select = _select;
		_callbacks.sigmaPlugins.lasso = _lasso;

		_callbacks.api.bindEvent = self.bindEvent.bind(self);
		_callbacks.api.unbindEvent = self.unbindEvent.bind(self);
		_callbacks.api.addNode = self.addNode.bind(self);
		_callbacks.api.addEdge = self.addEdge.bind(self);
		_callbacks.api.dropNode = self.dropNode.bind(self);
		_callbacks.api.dropEdge = self.dropEdge.bind(self);
		_callbacks.api.dropElement = self.dropElement.bind(self);
		_callbacks.api.updateNode = self.updateNode.bind(self);
		_callbacks.api.updateEdge = self.updateEdge.bind(self);

		return _s;
	};

	Graphbrowser.Control.SigmaControl.prototype.activate = function() {
		var self = this;
		self.isActive = true;
	};

	Graphbrowser.Control.SigmaControl.prototype.deactivate = function() {
		_callbacks.refreshSigma();
	};

	Graphbrowser.Control.SigmaControl.prototype.unbindEvent = function(event, handler) {
		if(!(typeof event === 'string') || !(typeof handler === 'function'))
			return;

		if(event === 'keyup'){
			var index = _keyupHandlers.indexOf(handler);
			_keyupHandlers.splice(index, 1);
		}
		if(event === 'keydown') {
			var index = _keydownHandlers.indexOf(handler);
			_keydownHandlers.splice(index, 1);
		}

		if(event.includes('drag'))
			_dragListener.unbind(event, handler);
		else
			_s.unbind(event, handler);
		//_callbacks.refreshSigma();
	};

	Graphbrowser.Control.SigmaControl.prototype.bindEvent = function(event, handler) {
		if(!(typeof event === 'string') || !(typeof handler === 'function'))
			return;

		if(event === 'keyup')
			_keyupHandlers.push(handler);
		if(event === 'keydown')
			_keydownHandlers.push(handler);

		if(event.includes('drag'))
			_dragListener.bind(event, handler);
		else
			_s.bind(event, handler);
		//_callbacks.refreshSigma();
	};

	Graphbrowser.Control.SigmaControl.prototype.addNode = function(node) {
		if(typeof node === 'string'){
			_s.graph.addNode({id:node, label: "", color: _s.settings('defaultNodeColor'), size: _s.settings('defaultNodeSize')});
		}
		else{
			_s.graph.addNode(node);
		}
		_s.refresh();
	};

	Graphbrowser.Control.SigmaControl.prototype.addEdge = function(edge, source, target) {
		if(source !== undefined && target !== undefined && (typeof edge === "string")){
			_s.graph.addEdge({id: edge, source: source, target: target, color: _s.settings('defaultEdgeColor'), size: _s.settings('defaultEdgeSize')})
		}
		else if(typeof edge === 'object'){
			_s.graph.addEdge(edge);
		}
		_s.refresh();

	};

	Graphbrowser.Control.SigmaControl.prototype.dropNode = function(node) {
		if(typeof node === "string"){
			if(_s.graph.nodes(node)){
				_s.graph.dropNode(node);
			}
		}
		else{
			if(_s.graph.nodes(node.id)){
				_s.graph.dropNode(node.id);
			}
		}

		_s.refresh();
	};

	Graphbrowser.Control.SigmaControl.prototype.dropEdge = function(node) {
		if(typeof edge === "string"){
			if(_s.graph.edges(edge)){
				_s.graph.dropEdge(edge);
			}
		}
		else{
			if(_s.graph.edges(edge.id)){
				_s.graph.dropEdge(edge.id);
			}
		}

		_s.refresh();
	};

	Graphbrowser.Control.SigmaControl.prototype.dropElement = function(element) {
		if(_s.graph.nodes(element)){
			_s.graph.dropNode(element);
		}

		else if(_s.graph.edges(element)){
			_s.graph.dropEdge(element);
		}

		_s.refresh();
	};

	Graphbrowser.Control.SigmaControl.prototype.updateNode = function(node, obj, fields, map) {
		var gNode = _s.graph.nodes(node);

		if(gNode === undefined)
			return;

		if(obj.id)
			gNode.id = obj.id;
		if(obj.label)
			gNode.label = obj.label;
		if(obj.color)
			gNode.color = obj.color;
		if(obj.size)
			gNode.size = obj.size;
		if(obj.nodeType)
			gNode.nodeType = obj.nodeType;
		if(obj.x)
			gNode.x = obj.x;
		if(obj.y)
			gNode.y = obj.y;

		if($.isArray(fields)){
			for(var i = 0; i < fields.length; i++){
				gNode[fields[i]] = obj[fields[i]];
			}
		}

		if(typeof map === 'object'){
			for(var mapPair in map){
				gNode[mapPair] = gNode[map[mapPair]];
			}
		}
		_s.refresh({skipIndexation: false});
	};

	Graphbrowser.Control.SigmaControl.prototype.updateEdge = function(edge, obj, fields, map) {
		var gEdge = _s.graph.edges(edge);

		if(gEdge === undefined)
			return;

		if(obj.id)
			gEdge.id = obj.id;
		if(obj.source)
			gEdge.source = obj.source;
		if(obj.target)
			gEdge.target = obj.target;
		if(obj.size)
			gEdge.size = obj.size;
		if(obj.type)
			gEdge.type = obj.type;
		if(obj.relType)
			gEdge.relType = obj.relType;
		if(obj.relName)
			gEdge.relName = obj.relName;
		if(obj.color)
			gEdge.color = obj.color;
		if(obj.label)
			gEdge.label = obj.label;

		if($.isArray(fields)){
			for(var i = 0; i < fields.length; i++){
				gEdge[fields[i]] = obj[fields[i]];
			}
		}

		if(typeof map === 'object'){
			for(var mapPair in map){
				gEdge[mapPair] = gNode[map[mapPair]];
			}
		}
		_s.refresh({skipIndexation: false});
	};


}).call(window);


