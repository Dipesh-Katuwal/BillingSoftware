import { createContext, useEffect } from "react";
import { categoriesAPI } from "../api/categoriesAPI";
import { useState } from "react";
import { AuthAPI } from "../api/AuthAPI";

export const AppContext = createContext(null);

export const AppContextProvider = (props) => {
  const [categories,setCategories] = useState([]);
  const [items,setItems] = useState([]);
  const [cartItems,setCartItems] = useState([]);
  const [customer,setCustomer] = useState({ name: "", phone: "" });
  const [users,setUsers] = useState([]);
  const [selectedCategoryId,setSelectedCategoryId] = useState(null);
  const [auth,setAuth] = useState(() => ({
    token: localStorage.getItem("token"),
    role: localStorage.getItem("role"),
    email: null,
  }))

  useEffect(() => {
    if (!auth.token) return;
    let ignore = false;
    AuthAPI.currentUser()
      .then(({ data }) => {
        if (!ignore) setAuth((current) => ({ ...current, email: data.email }));
      })
      .catch(() => console.warn("Could not load the signed-in email."));
    return () => { ignore = true; };
  }, [auth.token]);

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

  const setAuthData = (token,role,email = null) => {
    setAuth({token,role,email})
  }

  const contextValue = {
    categories,
    setCategories,
    items,
    setItems,
    cartItems,
    setCartItems,
    customer,
    setCustomer,
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
