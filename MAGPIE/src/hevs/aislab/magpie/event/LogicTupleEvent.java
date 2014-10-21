package hevs.aislab.magpie.event;

import hevs.aislab.magpie.environment.Services;
import alice.tuprolog.Term;

public class LogicTupleEvent extends MagpieEvent {

	private String logicRepresentation;
	
	public LogicTupleEvent(Term t) {
		this.type = Services.LOGIC_TUPLE;
		this.logicRepresentation = t.toString();	
	}
	
	public LogicTupleEvent(String name, String[] arguments) {
		this.type = Services.LOGIC_TUPLE;
		
		String tuple = "" + name + "(";
		
		for(int i=0; i<arguments.length; i++){
			tuple = tuple + arguments[i];
		}
		
		tuple = tuple + ")";
		this.logicRepresentation = tuple;
	}
	
	/**
	 * It gives the tuple representation of the event.
	 * 
	 * @return
	 */
	public String toTuple(){
		return this.logicRepresentation;
	}
}
