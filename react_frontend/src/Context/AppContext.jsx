import { createContext, useEffect } from "react";
import { categoriesAPI } from "../api/categoriesAPI";
import { useState } from "react";

export const AppContext = createContext(null);

export const AppContextProvider = (props) => {
  const [categories,setCategories] = useState([]);
  const [items,setItems] = useState([]);
  const [users,setUsers] = useState([]);
  const [selectedCategoryId,setSelectedCategoryId] = useState(null);
  const [auth,setAuth] = useState({token:null,role:null})

  useEffect(()=>{
    if (!localStorage.getItem("token")) return;

    const get_Categories = async () =>{
       const data = await categoriesAPI.getCategories();
       setCategories(data);
    }
    get_Categories().catch((error) => {
      console.error("Unable to load categories:", error);
    });
  },[auth.token])

  const setAuthData = (token,role) => {
    setAuth({token,role})
  }

  const contextValue = {
    categories,
    setCategories,
    items,
    setItems,
    users,
    setUsers,
    selectedCategoryId,
    setSelectedCategoryId,
    auth,
    setAuthData
  }

  return <AppContext.Provider value={contextValue}>
      {props.children}
  </AppContext.Provider>
}
