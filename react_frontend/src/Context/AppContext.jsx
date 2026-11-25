import { createContext, useEffect } from "react";
import { categoriesAPI } from "../api/categoriesAPI";
import { useState } from "react";

export const AppContext = createContext(null);

export const AppContextProvider = (props) => {
  const [categories,setCategories] = useState([]);
  const [auth,setAuth] = useState({token:null,role:null})

  useEffect(()=>{
    const get_Categories = async () =>{
       const data = await categoriesAPI.getCategories();
       setCategories(data);
    }
    get_Categories();
  },[])

  const setAuthData = (token,role) => {
    setAuth({token,role})
  }

  const contextValue = {
    categories,
    setCategories,
    auth,
    setAuthData
  }

  return <AppContext.Provider value={contextValue}>
      {props.children}
  </AppContext.Provider>
}
