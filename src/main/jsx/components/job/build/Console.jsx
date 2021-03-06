import React from "react";
import Router from 'react-router';
import mapIndexed from 'ramda/src/mapIndexed';
import Convert from  'ansi-to-html';
import LocationHashHelper from './../../mixins/LocationHashHelper.jsx'
require('./console.less');
export default React.createClass({
  mixins: [LocationHashHelper ], 
  componentDidMount(){
    this.addHashListener(this._onLineSelectionChange);
    this._scrollToLine(this.selectedHash());
  },
  componentWillUnmount(){
    this.removeHashListener(this._onLineSelectionChange);
  },
  _onLineSelectionChange(event){
    const [oldId, newId] = this.getHashIds(event);
    if(oldId){
      document.getElementById(oldId).classList.remove('highlight');
    }
    if(newId){
      document.getElementById(newId).classList.add('highlight');
    }
  },
  _isBuildLoaded(){
    return this.props.log && this.props.log.size > 1;
  },
  render(){
    return <span id="buildLog"><pre> {this._renderLog(this.props.log)}</pre></span>;
  },
  _onLineSelect(event){
    if(event.target.tagName == 'A'){
      event.stopPropagation();
      const lineId = event.currentTarget.getAttribute('id');
      Router.HashLocation.push(lineId);
    }
  },
  _scrollToLine(lineId){
    if(lineId){
      const line = document.getElementById(lineId);
      if(line)
        line.scrollIntoView();
    }
  },
  componentDidUpdate(){
    this._scrollToLine(this.selectedHash());
  },
  _isLineSelected(lineNumber){
    return `L${lineNumber}`  == this.selectedHash();
  },
  _openFold(e){
    e.stopPropagation();
    e.currentTarget.classList.toggle('closed');
    e.currentTarget.classList.toggle('open');
  },
  _logFold(log,idx,isOpen){
    return (
      <div key={'fold'+(idx)} className={"fold "+ (isOpen? "open":"closed")} onClick={this._openFold}>
        {mapIndexed((line,lineNo) => this._logLine(line,idx+lineNo),log)}
      </div>
    ) 
  },
  _logLine(log,idx){
    return (<p dangerouslySetInnerHTML={{__html: "<a></a>"+new Convert().toHtml(log)}}
      key={idx} className={this._isLineSelected(idx)?'highlight':''} id={`L${idx}`} onClick={this._onLineSelect}>
    </p>);
  },
  _renderLog(logLines){
    var groupedLines = [[]];
    for (let i = 0; i < logLines.size; i++) {
      let line =logLines.get(i);
      if(line.startsWith('$')){
        groupedLines.push([line]);
      }else{
        groupedLines[ groupedLines.length-1].push(line);
      }
    }
    var lineNo = 1;
    return mapIndexed((log,idx) => {
      if(log.length  == 1){
        const logLine =this._logLine(log[0],lineNo);
        lineNo = lineNo + 1;
        return logLine;
      }else{
        const selectedLine = this.selectedHash()!=''? parseInt(this.selectedHash().replace("L",'')) :0;
        var lineSelectedInFold = selectedLine > lineNo && selectedLine < lineNo + log.length;
        var fold = this._logFold(log,lineNo,idx == groupedLines.length -1 || lineSelectedInFold);
        lineNo = lineNo + log.length
        return fold;
      }
    },groupedLines); 
  }

});
