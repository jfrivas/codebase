package org.jbpt.petri;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Net system implementation
 * 
 * @author Artem Polyvyanyy
 */
public class NetSystem extends PetriNet {
	
	private Marking M = null;
	
	public NetSystem() {
		this.M = new Marking(this);
	}
	
	@Override
	public Node removeNode(Node n) {
		Node result = super.removeNode(n);
		if (result!=null && n instanceof Place) {
			this.M.remove((Place)n);
		}
		return result;
	}

	@Override
	public Collection<Node> removeNodes(Collection<Node> ns) {
		Collection<Node> result = super.removeNodes(ns);
		if (result!=null) {
			for (Node n : result) {
				if (n instanceof Place)
					this.M.remove((Place)n);
			}
		}
		return result;
	}

	@Override
	public Place removePlace(Place p) {
		Place result = super.removePlace(p);
		if (result!=null) {
			this.M.remove(p);
		}
		return result;
	}

	@Override
	public Collection<Place> removePlaces(Collection<Place> ps) {
		Collection<Place> result = super.removePlaces(ps);
		if (result!=null) {
			for (Place p : result) {
				this.M.remove(p);
			}
		}
		return result;
	}

	/**
	 * Get marking
	 * @return Marking of the net system
	 */
	public Marking getMarking() {
		return this.M;
	}
	
	/**
	 * Set marking
	 * @param m the marking of the net system
	 */
	public void setMarking(Marking m) {
		this.M = m;
	}

	/**
	 * Get marked places of the net system
	 * @return Marked places of the net system
	 */
	public Collection<Place> getMarkedPlaces() {
		return this.M.keySet();
	}
	
	/**
	 * Get enabled transitions of the net system
	 * @return Enabled transitions of the net system
	 */
	public Collection<Transition> getEnabledTransitions() {
		Collection<Transition> result = new ArrayList<Transition>();
		Set<Place> marked = new HashSet<Place>(this.getMarkedPlaces());
		
		for (Transition t : this.getTransitions()) {
			if (marked.containsAll(this.getPreset(t)))
				result.add(t);
		}
		
		return result;
	}
	
	/**
	 * Check if transition is enabled
	 * @param t Transition
	 * @return <code>true</code> if t is enabled; <code>false</code> otherwise
	 */
	public boolean isEnabled(Transition t) {
		for (Place p : this.getPreset(t))
			if (!this.isMarked(p))
				return false;
			
		return true;
	}
	
	/**
	 * Check if place is marked, i.e., contains at least one token
	 * @param p Place
	 * @return <code>true</code> if place is marked; <code>false</code> otherwise
	 */
	private boolean isMarked(Place p) {
		return this.M.isMarked(p);
	}
	
	/**
	 * Fire transition. 
	 * Transition fires only if it is enabled. 
	 * Firing removes one token from every preplace and adds one token to every postplace. 
	 * 
	 * @param t Transition to fire
	 * @return <code>true</code> if firing changed the marking; <code>false</code> otherwise
	 */
	public boolean fire(Transition t) {
		if (!this.isEnabled(t)) return false;
		
		for (Place p : this.getPreset(t))
			this.M.put(p, this.M.get(p)-1);
		
		for (Place p : this.getPostset(t))
			this.M.put(p, this.M.get(p)+1);
		
		return true;
	}

	@Override
	public String toDOT() {
		String result = "digraph G {\n";
		result += "graph [fontname=\"Helvetica\" fontsize=10 nodesep=0.35 ranksep=\"0.25 equally\"];\n";
		result += "node [fontname=\"Helvetica\" fontsize=10 fixedsize style=filled fillcolor=white penwidth=\"2\"];\n";
		result += "edge [fontname=\"Helvetica\" fontsize=10 arrowhead=normal color=black];\n";
		result += "\n";
		result += "node [shape=circle];\n";
		
		for (Place p : this.getPlaces()) {
			Integer n = this.M.get(p);
			String label = ((n == 0) || (n == null)) ? "" : n.toString(); 
			result += String.format("\tn%s[label=\"%s\" width=\".3\" height=\".3\"];\n", p.getId().replace("-", ""), label);
		}
		
		result += "\n";
		result += "node [shape=box];\n";
		
		for (Transition t : this.getTransitions()) {
			String fillColor = this.isEnabled(t) ? " fillcolor=\"#9ACD32\"" : "";
			if (t.getName()=="")
				result += String.format("\tn%s[label=\"%s\" width=\".3\""+fillColor+" height=\".1\"];\n", t.getId().replace("-", ""), t.getName());
			else 
				result += String.format("\tn%s[label=\"%s\" width=\".3\""+fillColor+" height=\".3\"];\n", t.getId().replace("-", ""), t.getName());
		}
		
		result += "\n";
		for (Flow f: this.getFlow()) {
			result += String.format("\tn%s->n%s;\n", f.getSource().getId().replace("-", ""), f.getTarget().getId().replace("-", ""));
		}
		result += "}\n";
		
		return result;
	}
	
	@Override
	public NetSystem clone() {
		NetSystem clone = (NetSystem) super.clone();
		
		/*
		 * Clone the marking 
		 */
		Marking cMarking = new Marking(clone);
		clone.setMarking(cMarking);
		
		/*
		 * Init marking according to original net system
		 */
		for (Place p : this.getMarkedPlaces()) {
			Place nPlace = null;
			for (Place n : clone.getPlaces())
				if (n.getId().equals(p.getId())) {
					nPlace = n;
					break;
				}
			
			assert(nPlace != null);
			
			clone.putTokens(nPlace, this.getTokens(p));
		}
		return clone;
	}
	
	public NetSystem clone(Map<Node,Node> nodeMapping) {
		NetSystem clone = this.clone();
		
		outer:
		for (Place p : this.getPlaces()) {
			for (Place cp : clone.getPlaces()) {
				if (p.getId().equals(cp.getId())) {
					nodeMapping.put(p, cp);
					continue outer;
				}
			}
		}
			
		outer:
		for (Transition t : this.getTransitions()) {
			for (Transition ct : clone.getTransitions()) {
				if (t.getId().equals(ct.getId())) {
					nodeMapping.put(t, ct);
					continue outer;
				}
			}
		}
		
		return clone;
	}
	
	/**
	 * Put tokens at a place
	 * @param p Place
	 * @param tokens Number of tokens to put
	 * @return the previous number of tokens at p, or <code>null</code> if parameters are wrong (either are equal to <code>null</code> or place does not belong to the net system, etc)
	 */
	public Integer putTokens(Place p, Integer tokens) {
		return this.M.put(p, tokens);
	}
	
	/**
	 * Get number of tokens at a place
	 * @param p Place
	 * @return Number of tokens at p
	 */
	public Integer getTokens(Place p) {
		return this.M.get(p);
	}
	
	/**
	 * Changes marking of the net system to its natural initial marking, i.e., 
	 * the marking which put one token at each source place of the net and no tokens elsewhere
	 */
	public void loadNaturalMarking() {
		this.M.clear();
		for (Place p : this.getSourcePlaces()) {
			this.M.put(p,1);
		}
	}
	
	/**
	 * Changes marking of the net system to the given marking
	 * @param M Marking
	 */
	public void loadMarking(Marking M) {
		this.M.clear();
		for (Map.Entry<Place,Integer> entry : M.entrySet()) {
			this.M.put(entry.getKey(),entry.getValue());
		}
	}
}