import { defineStore } from "pinia";
import { ref } from "vue";
import { api } from "../utils/api.js";

export const useRolesStore = defineStore("roles", () => {
  const roles = ref([]);
  const loaded = ref(false);

  async function fetchRoles() {
    try {
      const res = await api.get("/roles");
      roles.value = res.data;
      loaded.value = true;
    } catch (e) {
      console.error("获取角色列表失败", e);
    }
  }

  async function createRole(name, introText, avatarEmoji) {
    const res = await api.post("/roles", { name, introText, avatarEmoji });
    roles.value.push(res.data);
    return res.data;
  }

  async function deleteRole(id) {
    await api.delete(`/roles/${id}`);
    roles.value = roles.value.filter((r) => r.id !== id);
  }

  return { roles, loaded, fetchRoles, createRole, deleteRole };
});
