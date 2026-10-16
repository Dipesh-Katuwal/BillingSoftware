import { createContext, useEffect } from "react";
import { categoriesAPI } from "../api/categoriesAPI";
export const AppContext = createContext(null);
import { useState } from "react";

export const AppContextProvider = (props) => {
  const [categories,setCategories] = useState([]);
  useEffect(()=>{
    const get_Categories = async () =>{
       const data = await categoriesAPI.getCategories();
       setCategories(data);
    }
    get_Categories();
  },[])

  const contextValue = {
    categories
  }

  return <AppContext.Provider value={contextValue}>
      {props.children}
  </AppContext.Provider>
}
