package util;

import java.util.ArrayList;

public class Graph {
	private ArrayList<Vertex> vertices;
	private ArrayList<Edge> edges;
	private int dimX;
	private int dimY;

	public Graph(int rows, int columns) {
		vertices = new ArrayList<>();
		edges = new ArrayList<>();
		dimX = columns;
		dimY = rows;
	}
	
	public boolean haveIsolatedCores() {
		ArrayList<Vertex> alc = new ArrayList<Vertex>();
		//Escolha do 0.0 para ser o core inicial. Garantido a existencia do primeiro nodo em todas as topologias
		vertex("0.0").checkIsolation(alc);
		
		//Se lista de alcancaveis for igual ao total de cores nao existe isolamento
		if(!(alc.size()==vertices.size())) return true;
		
    	return false;
	}

	public ArrayList<Vertex> getVertices() {

		return this.vertices;

	}

	public ArrayList<Edge> getEdges() {
		return this.edges;
	}

	public Vertex vertex(String name) {
		Vertex vertex = null;

		for (Vertex v : this.vertices) {
			if (v.name().equals(name))
				vertex = v;
		}

		if (vertex == null) {
			System.out.println("Vertex: " + name + " nao encontrado");
			return null;
		}

		return vertex;
	}

	void addVertex(String nome) {
		Vertex v = new Vertex(nome);
		vertices.add(v);
	}
	
	void addEdge(Vertex origem, Vertex destino, String cor) {
		Edge e = new Edge(origem, destino, cor);
		origem.addAdjunct(e);
		edges.add(e);
	}
	
	void addEdge(Edge toAdd) {
		toAdd.source().adjuncts().add(toAdd);
		edges.add(toAdd);
	}
	
	void removeEdge(Edge toRemove) {
		toRemove.source().adjuncts().remove(toRemove);
		edges.remove(toRemove);
	}

	public String toString() {
		String r = "";
		System.out.println("Graph:");
		for (Vertex u : vertices) {
			r += u.name() + " -> ";
			for (Edge e : u.adjuncts()) {
				Vertex v = e.destination();
				r += v.name() + e.color() + ", ";
			}
			r += "\n";
		}
		return r;
	}
	
	public int dimX() {
		return dimX;
	}

	public int dimY() {
		return dimY;
	}

	public int indexOf(Vertex v) {
		return indexOf(v.name());
	}

	private int indexOf(String xy) {
		int x = Integer.parseInt(xy.split("\\.")[0]);
		int y = Integer.parseInt(xy.split("\\.")[1]);
		return x + y*this.dimX();
	}

}
