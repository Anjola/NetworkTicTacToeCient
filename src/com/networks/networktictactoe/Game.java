package com.networks.networktictactoe;

public class Game {

	public char myPiece; 
	public char otherPiece;
	final private char empty = ' ';
	final private char rows = 3;
	final private char cols = 3;
	private char[][] board;
	
	public Game(char piece)
	{
		this.myPiece = piece;
		this.otherPiece = ((myPiece == 'X')? 'O':'X' );
		board  = new char[rows][cols];
		clearBoard();
	}
	
	public boolean isEmpty(int x, int y ) 
	{
		return board[x][y] == empty;
	}
	
	public boolean setPosition(int x, int y, char piece)
	{
		if(isEmpty(x,y))
		{
			board[x][y] = piece;
			return true;
		}
		return false;
		
	}
	
	public void clearBoard()
	{
		for(int i = 0; i< rows; ++i)
		{
			for(int j = 0; j<cols; ++j){
				board[i][j] = empty;
			}
		}
		
		
	}
	
	public boolean isFull()
	{
		for(int i = 0; i< rows; ++i)
		{
			for(int j = 0; j<cols; ++j){
				if(board[i][j] == empty)
				{
					return false;
				}
			}
		}
		return true;
	}
	
	public boolean isWinner(char piece)
	{
		return ( isHor(piece) || isVer(piece) ||islDiag(piece) ||isrDiag(piece));
		
	}

	private boolean islDiag(char piece) {
		// TODO Auto-generated method stub
		for(int i = 0, j = 0; i <rows &&j<cols;++i,++j)
		{
			if(board[i][j] != piece)
				return false;
		}
		return true;
	}
	
	private boolean isrDiag(char piece) {
		// TODO Auto-generated method stub
		for(int i = rows-1, j = cols -1; i >= 0 &&j>= 0; --i, --j)
		{
			if(board[i][j] != piece)
				return false;
		}
		return true;
	}


	private boolean isVer(char piece) {
		// TODO Auto-generated method stub
		for(int i = 0; i< cols; ++i)
		{
			for(int j = 0; j<rows; ++j){
				if(board[j][i] != piece)
					return false;
			}
		}
		return true;
	}

	private boolean isHor(char piece) {
		// TODO Auto-generated method stub
		for(int i = 0; i< rows; ++i)
		{
			for(int j = 0; j<cols; ++j){
				if(board[i][j] != piece)
					return false;
			}
		}
		return true;
	}
}
