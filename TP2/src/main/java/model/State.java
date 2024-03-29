package model;

import java.util.ArrayList;
import java.util.List;

public class State {

    private List<Cell> modified;
    private List<Cell> checked;
    private Cell[][][] cells;
    private List<Cell> alive;
    private int dim;
    private boolean is3D;
    private Cell center;


    public State(int dim, boolean is3D) {
        this.modified = new ArrayList<>();
        this.checked = new ArrayList<>();
        this.alive = new ArrayList<>();
        this.cells = new Cell[dim][dim][dim];
        this.dim = dim;
        this.is3D = is3D;

        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                if(is3D) {
                    for (int k = 0; k < dim; k++) {
                        this.cells[i][j][k] = new Cell(i, j, k, false);
                    }
                } else {
                	this.cells[i][j][0] = new Cell(i, j, 0, false);
                }
            }
        }
    }

    public List<Cell> getModified() {
        return modified;
    }

    public Cell[][][] getCells() {
        return cells;
    }

    public Cell getCell(int x, int y, int z) {

        if(x < 0 || x >= dim || y < 0 || y >= dim  || z < 0 || z >= dim )
            return null;

        return cells[x][y][z];
    }

    public boolean isChecked(Cell cell) {
        return checked.contains(cell);
    }

    public void setChecked(Cell cell) {
        checked.add(cell);
    }

    public List<Cell> getMooreNeighbours(Cell cell, int radius) {

        List<Cell> neighbours = new ArrayList<>();
        Cell neighbour;

        int x = cell.getX();
        int y = cell.getY();
        int z = cell.getZ();

        for (int i = x - radius; i <= x + radius; i++) {
            for (int j = y - radius; j <= y + radius; j++) {
                if(is3D) {
                    for (int k = z - radius; k <= z + radius; k++) {
                        if(i != x || j != y || k != z) {
                            neighbour = getCell(i, j, k);

                            if(neighbour != null)
                                neighbours.add(neighbour);
                        }
                    }
                } else {
                    if(i != x || j != y) {
                        neighbour = getCell(i, j, 0);

                        if(neighbour != null)
                            neighbours.add(neighbour);
                    }
                }
            }
        }

        return neighbours;
    }

    public List<Cell> getVonNeumannNeighbours(Cell cell, int radius) {

        List<Cell> neighbours = new ArrayList<>();
        Cell neighbour;

        int x = cell.getX();
        int y = cell.getY();
        int z = cell.getZ();

        for (int i = -radius; i <= radius; i++) {
            for (int j = -radius; j <= radius; j++) {
                if(is3D) {
                    for (int k = -radius; k <= radius; k++) {
                        if(Math.abs(i)+ Math.abs(j) + Math.abs(k) <= radius &&
                                (i != 0 || j != 0 || k != 0)) {
                            neighbour = getCell(x + i, y + j, z + k);

                            if(neighbour != null)
                                neighbours.add(neighbour);
                        }
                    }
                } else if (Math.abs(i) + Math.abs(j) <= radius &&
                        (i != 0 || j != 0)) {
                    neighbour = getCell(x + i, y + j, 0);

                    if(neighbour != null)
                        neighbours.add(neighbour);
                }
            }
        }

        return neighbours;
    }

    public void changeState(List<Cell> modified) {
        for (Cell cell: modified) {
            int x = cell.getX();
            int y = cell.getY();
            int z = cell.getZ();

            if(cell.isAlive()) {
                alive.remove(cell);
            } else {
                alive.add(cell);
            }

            this.cells[x][y][z].changeStatus();
        }

        this.modified = new ArrayList<>(modified);
        this.checked.clear();
    }

    public double getRadius() {
        double maxRadius = 0;
        double radius;

        for (Cell cell : this.alive) {
            radius = cell.getDistanceToOrigin(center);

            if(radius > maxRadius) {
                maxRadius = radius;
            }
        }

        return maxRadius;
    }

    public int getAliveCount() {
        return alive.size();
    }

    public List<Cell> getAlive() {
        return alive;
    }

    public int getDim() {
    	return dim;
    }
    
    public boolean is3D() {
    	return is3D;
    }

    public void setCenter() {

        double x = 0;
        double y = 0;
        double z = 0;

        for (Cell cell : this.alive) {
            x += cell.getX();
            y += cell.getY();
            z += cell.getZ();
        }

        center = new Cell((int) (x / alive.size()),(int) (y / alive.size()), (int)(z / alive.size()), false);
    }
}
