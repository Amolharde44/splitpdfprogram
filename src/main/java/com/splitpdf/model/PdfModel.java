package com.splitpdf.model;

public class PdfModel {
	
	private int startPage;
	private int endPage;
	public PdfModel(int startPage, int endPage) {
		super();
		this.startPage = startPage;
		this.endPage = endPage;
	}
	public PdfModel() {
		super();
		// TODO Auto-generated constructor stub
	}
	public int getStartPage() {
		return startPage;
	}
	public void setStartPage(int startPage) {
		this.startPage = startPage;
	}
	public int getEndPage() {
		return endPage;
	}
	public void setEndPage(int endPage) {
		this.endPage = endPage;
	}
	@Override
	public String toString() {
		return "PdfModel [startPage=" + startPage + ", endPage=" + endPage + "]";
	}

	
}
