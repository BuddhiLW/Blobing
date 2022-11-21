package main

import (
	"log"
	"net/http"

	"github.com/julienschmidt/httprouter"
)

func (app *application) sfile(w http.ResponseWriter, req *http.Request) {
	http.ServeFile(w, req, "cv-2022-11-03.pdf")
	log.Println("Served File")
}

func (app *application) routes() *httprouter.Router {
	r := httprouter.New()

	r.HandlerFunc(http.MethodGet, "/file", app.sfile)

	return r
}
