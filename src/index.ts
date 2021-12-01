import { Cs108Module } from './Cs108Module';

export class Cs108Manager {
  _id: string;

  constructor() {
    this._id = 'testID';
  }

  getId() {
    return Cs108Module.createClient();
  }
}
