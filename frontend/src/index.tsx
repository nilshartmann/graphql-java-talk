import { ApolloProvider } from "@apollo/react-hooks";
import React from "react";
import ReactDOM from "react-dom";
import { AuthProvider } from "./AuthContext";
import BeerRatingApp from "./BeerRatingApp";
import createApolloClient from "./createApolloClient";

const client = createApolloClient();

const theBeerRatingApp = (
  <ApolloProvider client={client}>
    <AuthProvider>
      <BeerRatingApp />
    </AuthProvider>
  </ApolloProvider>
);

const mountNode = document.getElementById("root");
ReactDOM.render(theBeerRatingApp, mountNode);
